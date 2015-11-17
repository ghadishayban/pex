(ns com.champbacon.pex.impl.codegen
  (:import com.champbacon.pex.impl.OpCodes
           (com.champbacon.pex ParsingExpressionGrammar CharMatcher ParseAction)))

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
        (conj [:commit cont] [:label cont]))))

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

(defn emit-capture
  [env ast]
  ;; optimize
  (let [body (concat-trees env (:children ast))]
    (-> (into [[:begin-capture]] body)
        (conj [:end-capture]))))

(defn emit-linked-instruction
  [k env ast]
  (let [linked-constant (-> ast :args first keyword)
        n (or (get-in env [:constants k linked-constant])
              (throw (ex-info "Linked constant not found" ast)))]
    [[k n]]))

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
               :capture emit-capture
               :action  (partial emit-linked-instruction :action)
               :charset (partial emit-linked-instruction :charset)})

(defn emit
  [env ast]
  (let [f (dispatch (:op ast))]
    (when-not f (throw (ex-info "bad ast" ast)))
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
  (let [m {:call OpCodes/CALL
               :return OpCodes/RET
               :choice OpCodes/CHOICE
               :commit OpCodes/COMMIT
               :partial-commit OpCodes/PARTIAL_COMMIT
               :back-commit  OpCodes/BACK_COMMIT
               :jump OpCodes/JUMP
               :fail-twice  OpCodes/FAIL_TWICE
               :fail   OpCodes/FAIL
               :end  OpCodes/END

               :char  OpCodes/MATCH_CHAR
               :test-char  OpCodes/TEST_CHAR
               :charset OpCodes/CHARSET
               :test-charset OpCodes/TEST_CHARSET
               :any OpCodes/ANY
               :test-any OpCodes/TEST_ANY
               :span OpCodes/SPAN

               :begin-capture OpCodes/BEGIN_CAPTURE
               :end-capture OpCodes/END_CAPTURE
               :full-capture OpCodes/FULL_CAPTURE
           :behind OpCodes/BEHIND
           :end-of-input OpCodes/END_OF_INPUT
           :action OpCodes/ACTION}]
    (fn [kw]
      (or (get m kw)
          (throw (IllegalArgumentException. (str "No opcode defined " kw)))))))

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
  [grammar matchers actions]
  (let [current-id (atom 0)]
    {:non-terminals (set (keys grammar))
     :next-label #(swap! current-id inc)
     :matchers (vec (vals matchers))
     :actions (vec (vals actions))
     :constants {:charset (into {} (map vector (keys matchers) (range)))
                 :action  (into {} (map vector (keys actions) (range)))}}))

(defn transform-instructions
  [insts]
  (let [->bytecode (fn [i]
                     (if (keyword? i)
                       (op->code i)
                       i))]
    (into [] (map ->bytecode) insts)))

(defn compile-grammar
  [grammar entrypoint matchers actions]
  (let [env (empty-env grammar matchers actions)
        emit-rule (fn [[sym ast]]
                    (-> (into [[:label sym :call]]
                              (emit env ast))
                        (conj [:return])))
        instructions (into [] (mapcat emit-rule) grammar)]
    (ParsingExpressionGrammar.
      (-> (add-entrypoint env instructions entrypoint)
           (link)
           (transform-instructions)
           (int-array))
      (into-array CharMatcher (:matchers env))
      (into-array ParseAction (:actions env)))))
