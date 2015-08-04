(ns com.champbacon.impl.codegen)

(declare emit)

(defn label
  [env]
  ((:next-label env)))

(defn emit-choice
  [env ast]
  (let [{:keys [children]} ast
        n (count children)
        last-pos? (fn [i] (= i (dec n)))

        labels (into [] (take n) (repeatedly #(label env)))
        blocks (mapv (partial emit env) children)
        
        end (label env)
        
        emit-alternative (fn [idx tree]
                           (let [header (when-not (zero? idx)
                                          [[:label (labels idx)]])
                                 choice (when-not (last-pos? idx)
                                          [[:choice (labels (inc idx))]])
                                 commit (when-not (last-pos? idx)
                                          [[:commit end]])]
                             (into [] cat
                                   [header
                                    choice
                                    (blocks idx)
                                    commit])))]
    (-> (into [] (comp (map-indexed emit-alternative) cat) children)
        (conj [:label end]))))

(defn concat-trees
  [env ts]
  (into [] (mapcat (partial emit env)) ts))

(defn emit-cat
  [env ast]
  (concat-trees env (:children ast)))

(defn emit-char
  [env ast]
  (let [{:keys [codepoint]} ast]
    [[:char codepoint]]))

(defn emit-rep
  [env ast]
  (let [body (concat-trees env (:children ast))
        head (label env)
        cont (label env)]
    (into [] cat
          [[[:choice cont]
            [:label head]]
           body
           [[:partial-commit head]
            [:label cont]]])))

(defn emit-call
  [env ast]
  (let [{:keys [target]} ast]
    (when-not (contains? (:non-terminals env) target)
      (throw (ex-info "Undefined non-terminal" {:target target})))
    [[:call target]]))

(defn emit-optional
  [env ast]
  (let [body (concat-trees env (:children ast)) 
        cont (label env)]
    (-> (into [[:choice cont]] body)
        (conj [:label cont]))))

(defn emit-not-predicate
  [env ast]
  (let [body (concat-trees env (:children ast))
        L1 (label env)]
    (-> (into [[:choice L1]] body)
        (conj [:fail-twice]
              [:label L1]))))

(defn emit-and-predicate
  [env ast]
  (let [body (concat-trees env (:children ast))
        L1 (label env)
        L2 (label env)]
    (-> (into [[:choice L1]] body)
        (into [[:back-commit L2]
               [:label L1]
               [:fail]
               [:label L2]]))))

(defn emit-range
  [_ ast]
  (let [{:keys [args]} ast]
    [[:range (first args)]]))

(defn emit-capture
  [env ast]
  ;; optimize
  (let [body (concat-trees env (:children ast))]
    (-> (into [[:begin-capture]] body)
        (conj [:end-capture]))))

(def dispatch {:choice emit-choice
               :char emit-char
               :cat  emit-cat
               :rep emit-rep
               :open-call emit-call
               :optional emit-optional
               :true (constantly [])
               :fail (constantly [[:fail]])
               :any (constantly [[:any]])
               :end-of-input (constantly [[:end-of-input]])
               :not emit-not-predicate
               :and emit-and-predicate
               :range emit-range
               :capture emit-capture
               #_(
                  :action
                  :reduce
                  :action

                  :push)})

(defn emit
  [env ast]
  (let [f (dispatch (:op ast))]
    (f env ast)))

(defn initial-jump-block
  [instrs entrypoint]
  (let [preamble [[:call entrypoint]
                  [:end]]]
    (into preamble instrs)))

(defn empty-env
  [grammar]
  (let [current-id (atom 0)]
    {:non-terminals (set (keys grammar))
     :next-label #(swap! current-id inc)}))

(def branching?
  #{:commit
    :choice
    :jump
    :call
    :back-commit
    :partial-commit})

(defn link
  [instructions]
  (let [[insts labels] (reduce (fn [[insts labels] [op arg :as inst]]
                                 (if (= :label op)
                                   [insts (assoc labels arg (count insts))]
                                   [(conj insts inst) labels]))
                               [[] {}] instructions)

        redirect (fn [[op arg :as inst] pc]
                   (if (branching? op)
                     (assoc inst 1 (if-let [dest (labels arg)] (- dest pc) arg))
                     inst))]
    (mapv redirect insts (range))))

(defn add-entrypoint
  [env code entrypoint]
  (let [end (label env)]
    (concat [[:call entrypoint]
             [:jump end]]
            code
            [[:label end]
             [:end]])))

(defn emit-instructions
  [grammar entrypoint]
  (let [env (empty-env grammar)

        instructions (into []
                           (mapcat (fn [[kw ast]]
                                     (-> (into [[:label kw :call]]
                                               (emit env ast))
                                         (conj [:return]))))
                           grammar)]
    (link (add-entrypoint env instructions entrypoint))))
