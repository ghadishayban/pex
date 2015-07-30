(ns com.champbacon.pex)



;; vm

(defn op
  [inst & extra]
  {:op inst
   :extra extra})

(defn jump
  [cur dest]
  (op :jmp (- dest cur)))

(def instructions
  #{:jmp
    :predicate
    :call
    :ret})

(defn run-step [prog sp captures])

(defn concat-blocks
  [& blocks]
  (into [] cat blocks))

(defn choice [p1 p2]
  (let [backtrack (inst :choice 'L1)
        commit (inst :commit 'L2)]
    (concat-blocks [backtrack]
                   (instructions p1)
                   [commit]
                   [(marker 'L1)]
                   (instructions p2)
                   [marker 'L2])))

;; ADDS to Stack
:push    ;; puts an abitrary val onto the value stack
:capture ;; puts the full text match onto the value stack

;; transforms stack
:action  ;; #{:reduce :call}  ;; call takes a # of args
:reduce ([] [r] [r i])
;; REMOVES from stack


'{NormalChar [[! QuoteBackslash] ANY [:fn appendLastChar]]
  Characters [[* NormalChar / ["\\" EscapedChar]]]
  JsonTrue ["true" [:push :js/true]]}
