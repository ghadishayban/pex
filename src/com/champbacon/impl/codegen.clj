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

(defn emit-action
  [env ast]
  (let [n (or (get (:actions env) (keyword (:target ast)))
              (throw (ex-info "Action not found" ast)))]
    [[:action n]]))

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
               :action emit-action})

(defn emit
  [env ast]
  (let [f (dispatch (:op ast))]
    (f env ast)))

(defn initial-jump-block
  [instrs entrypoint]
  (let [preamble [[:call entrypoint]
                  [:end]]]
    (into preamble instrs)))

(def branching?
  #{:commit
    :choice
    :jump
    :call
    :back-commit
    :partial-commit})

(def op->code
  (let [insts [:call
               :ret
               :choice
               :commit
               :partial-commit
               :back-commit
               :jump
               :fail-twice
               :fail
               :end

               :char
               :test-char
               :charset
               :test-charset
               :any
               :test-any
               :span

               :begin-capture
               :end-capture
               :full-capture
               :behind]]
    (into {} (map-indexed (fn [i op] [op i]))
          insts)))

(defn link
  "Turns all symbolic jumps into relative address jumps"
  [instructions]
  (let [[insts labels] (reduce (fn [[insts labels] [op arg :as inst]]
                                 (if (= :label op)
                                   [insts (assoc labels arg (count insts))]
                                   [(into insts inst) labels]))
                               [[] {}] instructions)

        patch-jumps (fn [stream]
                      (let [n (count stream)]
                        (loop [i 0 stream stream]
                          (if (< i n)
                            (let [op (get stream i)]
                              (if (and (keyword? op) (branching? op))
                                (let [target (inc i)]
                                  (recur (inc target) (update stream target labels)))
                                (recur (inc i) stream)))
                            stream))))]
    (patch-jumps insts)))

(defn add-entrypoint
  [env code entrypoint]
  (let [end (label env)]
    (concat [[:call entrypoint]
             [:jump end]]
            code
            [[:label end]
             [:end]])))

(defn empty-env
  [grammar actions]
  (let [current-id (atom 0)]
    {:non-terminals (set (keys grammar))
     :next-label #(swap! current-id inc)
     :actions (into {} (map vector (keys actions) (range)))
     :action-impls (vec (vals actions))}))

(defn emit-instructions
  [grammar actions entrypoint]
  (let [env (empty-env grammar actions)

        emit-rule (fn [[sym ast]]
                    (-> (into [[:label sym :call]]
                              (emit env ast))
                        (conj [:return])))
        instructions (into [] (mapcat emit-rule) grammar)]
    (link (add-entrypoint env instructions entrypoint))))
