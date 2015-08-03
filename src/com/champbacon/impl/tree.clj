(ns com.champbacon.impl.tree
  (:refer-clojure :exclude [cat char not and]))

(def ^:dynamic *macros* {})

(defprotocol OpTree
  (pattern [_]))

;; make this work on pairs and do associativity fix inline?
(defn choice
  [alternatives]
  (case (count alternatives)
    0 (throw (ex-info "Empty ordered choice operator" {}))
    1 (first alternatives)
    {:op :choice
     :children alternatives}))

;; make this work on pairs and do associativity fix inline?
(defn cat
  [ps]
  (if (= 1 (count ps))
    (first ps)
    {:op :cat
     :children ps}))

(defn char-range
  [low high]
  {:op :range
   :low low
   :high high})

(defn char
  [codepoint]
  {:op :char
   :codepoint codepoint})

(defn string
  [s]
  (if (pos? (count s))
    (cat (mapv (comp char int) s))
    {:op :true}))

(def fail
  {:op :fail})

(defn rep
  [patt]
  {:op :rep
   :children patt})

(defn any
  ([] {:op :any})
  ([n] (cat (vec (repeat n {:op :any})))))

(defn not
  [patt]
  {:op :not
   :children patt})

(defn and
  [patt]
  {:op :and
   :children and})

(def end-of-input 
  {:op :end-of-input})

(defn action
  [])

(defn non-terminal
  [kw]
  {:op :open-call
   :target kw})

(defn push
  [obj]
  {:op :push
   :value obj})

(defn capture
  [ps]
  {:op :capture
   :children ps})

(defn optional
  [ps]
  {:op :optional
   :children ps})

(extend-protocol OpTree
  String
  (pattern [s] (string s))

  clojure.lang.IPersistentVector
  (pattern [v] (cat (mapv pattern v)))

  clojure.lang.Symbol
  (pattern [s]
    (condp = s
      'any (any)

      'EOI end-of-input

      (throw (ex-info "Unrecognized symbol" {:symbol s}))))

  java.lang.Character
  (pattern [ch] (char (int ch)))

  clojure.lang.Keyword
  (pattern [kw]
    (non-terminal kw))

  java.lang.Boolean
  (pattern [b]
    (if b
      {:op :true}
      {:op :fail}))
  
  clojure.lang.IPersistentList

  ;; TODO Add macroexpansion
  (pattern [l]
    (when-first [call l]
      (let [args (next l)
            call-with-args #(%1 (mapv pattern args))]
        (condp = call

          '/
          (call-with-args choice)

          'ANY
          (call-with-args any)

          '*
          (call-with-args rep)

          '?
          (call-with-args optional)

          'not
          (call-with-args not)
          
          'and
          (call-with-args and)

          'capture
          (call-with-args capture)

          'push
          (push (fnext l))

          ;;'reduce
          ;;(reduce-value-stack (next l))

          ;;'action
          ;;(action (next l))

          :else 
          (throw (ex-info "Unrecognized call" {:op call :form l})))))))

;;; Optimizations

(defn has-capture?
  [op]
  )

(defn empty-string-behavior
  [tree]
  ;; A pattern is *nullable* if it can match without consuming any character;
  ;; A pattern is *nofail* if it never fails for any string
  ;; (including the empty string).
 )

(defn fixed-length
  [tree]
  ;; number of characters to match a pattern (or nil if variable)
;; avoid infinite loop with a MAXRULES traversal count

  )

(defn first-set
  [tree]
  ;; https://github.com/lua/lpeg/blob/2960f1cf68af916a095625dfd3e39263dac5f38c/lpcode.c#L246
  )

(defn head-fail?
  [tree]
  ;; https://github.com/lua/lpeg/blob/2960f1cf68af916a095625dfd3e39263dac5f38c/lpcode.c#L341
  )

(defmacro show
  [form]
  `(clojure.pprint/pprint (pattern (quote ~form))))

(defn grammar
  [m entry-point])

