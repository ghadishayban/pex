(ns com.champbacon.pex
  (:refer-clojure :exclude [compile])
  (:require [com.champbacon.impl.tree :as tree]
            [com.champbacon.impl.codegen :as codegen]))

;; ADDS to Stack
;; :push    ;; puts an abitrary val onto the value stack
;; :capture ;; puts the full text match onto the value stack

;; transforms stack
;; :action  ;; #{:reduce :call}  ;; call takes a # of args
;; :reduce ([] [r] [r i])
;; REMOVES from stack

(defn match
  [vm input])

(defn compile
  ([data entrypoint]
    (compile data entrypoint {}))
  ([data entrypoint macros]
   (when-not (contains? data entrypoint)
     (throw (ex-info "Unknown entrypoint" {:grammar data
                                           :entrypoint entrypoint})))
   (let [ast (tree/parse-grammar data macros)]
     (codegen/emit-instructions ast entrypoint))))
