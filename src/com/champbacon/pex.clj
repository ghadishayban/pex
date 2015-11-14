(ns com.champbacon.pex
  (:refer-clojure :exclude [compile])
  (:require [com.champbacon.impl.tree :as tree]
            [com.champbacon.impl.codegen :as codegen]))

(defn compile
  ([data entrypoint actions]
    (compile data entrypoint actions {}))
  ([data entrypoint actions macros]
   (when-not (contains? data entrypoint)
     (throw (ex-info "Unknown entrypoint" {:grammar data
                                           :entrypoint entrypoint})))
   (let [ast (tree/parse-grammar data macros)]
     (codegen/emit-instructions ast actions entrypoint))))

(defn print-instructions
  [insts]
  (println "ADDR INST")
  (doseq [[idx inst] (map vector (range) insts)]
    (printf "%4d %s%n" idx inst)))

(comment
  (doto 'com.champbacon.pex require in-ns)
  (import com.champbacon.pex.impl.PEGByteCodeVM)
  (def simple '{email [(capture user) "@" (capture domain)]
                user   (/ "ghadi.shayban" "macro" "ninewest")
                domain "pokitdok.com"})
  (def instructions (compile simple 'email {} {}))
  (def is (int-array (codegen/transform-instructions instructions)))
  (def inp (.toCharArray "macro@pokitdok.com"))
  (def vm (PEGByteCodeVM. is nil nil inp nil))

  (.match vm)
  )
