(ns com.champbacon.pex
  (:refer-clojure :exclude [compile])
  (:require [com.champbacon.pex.impl.tree :as tree]
            [com.champbacon.pex.impl.codegen :as codegen])
  (:import (com.champbacon.pex.impl Matchers$SingleRangeMatcher
                                    Matchers$RangeMatcher
                                    Actions
                                    Actions$PushAction
                                    Actions$UpdateStackTop
                                    Actions$FoldCaptures
                                    Actions$ReplaceCaptures)
           (com.champbacon.pex ParsingExpressionGrammar)))

(defn single-range-matcher
  [low high]
  (Matchers$SingleRangeMatcher. (int low) (int high)))

(defn range-matcher
  [ranges]
  (let [nums (int-array (sequence (comp cat (map int)) (sort-by first ranges)))]
    (Matchers$RangeMatcher. nums)))

(defn update-stack-top
  [f]
  (Actions$UpdateStackTop. f))

(defn push
  [val]
  (Actions$PushAction. val))

(defn replace-captures
  "f will be passed array, low-idx, high-idx.
   The extent of captures will be replaced with the result of f
   high-idx is exclusive"
  [f]
  (Actions$ReplaceCaptures. f))

(def clear-sb Actions/CLEAR_STRING_BUFFER)
(def append-sb Actions/APPEND_STRING_BUFFER)
(def push-sb Actions/PUSH_STRING_BUFFER)

(defn fold-cap
  [rf]
  (Actions$FoldCaptures. rf))

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
     (codegen/compile-grammar ast entrypoint matchers actions))))

(defn matcher
  ([peg input]
    (matcher peg input nil))
  ([^ParsingExpressionGrammar peg input user-parse-context]
    (.matcher peg input user-parse-context)))

(defn print-instructions
  [insts]
  (println "ADDR INST")
  (doseq [[idx inst] (map vector (range) insts)]
    (printf "%4d %s%n" idx inst)))
