(ns com.champbacon.scratch
  (:refer-clojure :exclude [cat char not and]))

(defprotocol OpTree
  (pattern [_]))

(defn choice
  [alternatives]
  {:op :choice
   :children alternatives})

(defn cat
  [ps]
  {:op :cat
   :children ps})

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
  [min max patt]
  ;;
  )

(defn any
  ([] {:op :any})
  ([n] (cat (vec (repeat n {:op :any})))))

(defn not
  [patt]
  {:op :not
   :children [patt]})

(defn and
  [patt]
  {:op :and
   :children [patt]})

(def end-of-input 
  {:op :end-of-input})

(defn action
  [])

(defn non-terminal
  [kw]
  {:op :open-call
   :target kw})

(extend-protocol OpTree
  String
  (pattern [s] (string s))

  clojure.lang.IPersistentVector
  (pattern [v] (cat (mapv pattern v)))

  clojure.lang.Symbol
  (pattern [s]
    (condp = s
      'ANY
      (any)

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
      {:op :false}))
  
  clojure.lang.IPersistentList
  (pattern [l]
    (when-first [call l]
      (cond

        (= call '/)
        (choice (mapv pattern (next l)))


        (= call 'ANY)
        (apply any (next l))
        ;; (= call 'cat)
                
        ;; (= call 'push)

        ;; (= call 'not)
        
        ;; (= call 'and)

        ;; (= call 'capture)

        ;; (= call 'reduce)

        ;; (= call 'action)

        :else
        (throw (ex-info "Unrecognized call" {:op call :form l}))))))

