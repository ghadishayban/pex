(ns com.champbacon.pex.examples.json
  (:require [com.champbacon.pex :as pex])
  (:import (com.champbacon.pex ParseAction CharMatcher)))

(def JSON '{json  [whitespace value EOI]                    ;; Main Rule

            value (/ string number object array jtrue jfalse jnull)

            object  [(:sp "{")                              ;; sp is a rule macro that chews whitespace
                     (? (:join [string (:sp ":") value] (:sp ",")))
                     (:sp "}")
                     (action capture-object)]

            array [(:sp "[") (? (:join value (:sp ","))) (:sp "]") (action capture-array)]

            number [(:sp (capture integer (? frac) (? exp))) (action cast-number)]

            string [\" (action clear-sb) characters (:sp \") (action push-sb)]

            characters (* (/ [(not (/ \" \\)) any (action append-sb)]
                             [\\ escaped-character]))

            escaped-character (/ [(class escape) (action append-escape)]
                                 unicode)
            unicode ["u"
                     (capture (class hexdigit) (class hexdigit) (class hexdigit) (class hexdigit))
                     (action append-hexdigit)]

            integer [(? "-") (/ [(class digit19) digits]
                                (class digit))]
            digits  [(class digit) (* (class digit))]
            frac    ["." digits]
            exp     [(/ "e" "E") (? (/ "+" "-")) digits]
            jtrue  ["true"  whitespace (action push-true)]
            jfalse ["false" whitespace (action push-false)]
            jnull  ["null"  whitespace (action push-nil)]
            whitespace (* (class whitespace))})

(def json-macros {:sp   (fn [patt] [patt 'whitespace])
                  :join (fn [patt sep] [patt (list '* sep patt)])})

(defn make-json-object
  [^objects captures low high]
  (loop [i low
         m (transient {})]
    (if (< i high)
      (recur (unchecked-add i 2)
             (assoc! m (aget captures i)
                       (aget captures (unchecked-inc i))))
      (persistent! m))))

(defn json-parser
  []
  (let [escapes {\b \backspace
                 \f \formfeed
                 \n \newline
                 \r \return
                 \t \tab
                 \\ \\
                 \/ \/
                 \" \"}
        matchers {:digit19    (pex/single-range-matcher \1 \:)
                  :digit      (pex/single-range-matcher \0 \:)
                  :hexdigit   (pex/range-matcher [[\a \g] [\0 \:]])
                  :escape     (reify CharMatcher
                                (match [_ ch]
                                  (> (.indexOf "bt" ch) 0)))
                  :whitespace (reify CharMatcher
                                (match [_ ch]
                                  (Character/isWhitespace ch)))}
        actions {:append-hexdigit (reify ParseAction
                                    (execute [_ vsm]
                                      (let [^StringBuffer sb (.getUserParseContext vsm)
                                            captures (.getCurrentCaptures vsm)
                                            top (.getCaptureEnd vsm)
                                            hex (aget captures top)]
                                        (.append sb (char (Integer/parseInt hex 16)))
                                        (.setCaptureEnd vsm (dec top)))))
                 :capture-object  (pex/replace-captures make-json-object)
                 :capture-array   (pex/fold-cap (fn
                                                  ([] (transient []))
                                                  ([res] (persistent! res))
                                                  ([res input] (conj! res input))))
                 :append-escape   (reify ParseAction
                                    (execute [_ vsm]
                                      (let [^StringBuffer sb (.getUserParseContext vsm)
                                            last-ch (.getLastMatch vsm)]
                                        (.append sb ^char (escapes last-ch)))))
                 :cast-number     (pex/update-stack-top #(Double/valueOf ^String %))
                 :push-true       (pex/push true)
                 :push-false      (pex/push false)
                 :push-nil        (pex/push nil)
                 :clear-sb        pex/clear-sb
                 :append-sb       pex/append-sb
                 :push-sb         pex/push-sb}]
    (pex/compile JSON 'json matchers actions json-macros)))

(comment
  (let [json (json-parser)
        ;;input "\"42\""
        input "{\"bar\": [\"this\", 42, {}, [1,2,3], \"foo\"]}"
        m (pex/matcher json (.toCharArray input) (StringBuffer.))
        result (.match m 0)]
    (first (.getCaptures m))))