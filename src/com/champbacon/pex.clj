(ns com.champbacon.pex
  (:refer-clojure :exclude [compile])
  (:require [com.champbacon.impl.tree :as tree]
            [com.champbacon.impl.codegen :as codegen]))

(defn compile
  ([grammar entrypoint matchers]
    (compile grammar entrypoint matchers {} {}))
  ([grammar entrypoint matchers actions]
    (compile grammar entrypoint matchers actions {}))
  ([grammar entrypoint matchers actions macros]
   (when-not (contains? grammar entrypoint)
     (throw (ex-info "Unknown entrypoint" {:grammar grammar
                                           :entrypoint entrypoint})))
   (let [ast (tree/parse-grammar grammar macros)]
     (codegen/emit-instructions ast entrypoint matchers actions))))

(defn print-instructions
  [insts]
  (println "ADDR INST")
  (doseq [[idx inst] (map vector (range) insts)]
    (printf "%4d %s%n" idx inst)))

(comment
  (doto 'com.champbacon.pex require in-ns)
  (import com.champbacon.pex.ParsingExpressionGrammar)
  (def simple '{email [(capture user) "@" (capture domain)]
                user   (/ "ghadi.shayban" "macro" "ninewest")
                domain "pokitdok.com"})
  (def instructions (compile simple 'email {} {} {}))
  (def is (int-array (codegen/transform-instructions instructions)))
  (def input (.toCharArray "macro@pokitdok.com"))

  (def peg (ParsingExpressionGrammar. is nil nil))
  (def matcher (.matcher peg input nil))

  (.match matcher)
  )
