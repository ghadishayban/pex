(ns com.champbacon.json
  (:require [com.champbacon.pex :as pex]))

(def JSON '{json    [whitespace value EOI]

            value (/ string number object array jtrue jfalse jnull)

            object  [(:ws "{")
                     (:join [string (:ws ":") value] ",")
                     (:ws "}")
                     (action capture-object)]

            array [(:ws "[") (:join value (:ws ",")) (:ws "]")]

            number [(:ws (capture integer (? frac) (? exp))) (action cast-number)]

            string [\" (action clear-sb) characters (:ws \") (action push-sb)]

            characters (/ (* normal-character)
                          [\\ escaped-character])

            quote-backslash "\"\\"
            normal-character [(not quote-backslash) any (action append-sb)]
            escaped-character (/ quote-backslash
                                 "b"
                                 "f"
                                 "n"
                                 "r"
                                 "t"
                                 unicode)
            unicode ["u"
                     (capture (class hexdigit)
                              (class hexdigit)
                              (class hexdigit)
                              (class hexdigit))
                     (action append-hexdigit)]


            integer [(? "-") (/ [(class digit19) digits]
                                (class digit))]
            digits  [(class digit) (* (class digit))]
            frac    ["." digits]
            exp     [(:ignore-case "e") (? (:anyof "+-")) digits]
            jtrue  ["true"  whitespace (action push-true)]
            jfalse ["false" whitespace (action push-false)]
            jnull  ["null"  whitespace (action push-nil)]
            whitespace (* (:anyof " \n\r\t\f"))})

;; case(syms...)  tail recursion

(def json-macros {:anyof       (fn [str] (apply list '/ (seq str)))
                  :ws          (fn [patt] [patt 'whitespace])
                  :join        (fn [patt sep] [patt (list '* sep patt)])
                  :ignore-case identity})

(defn make-json-object
  [captures low high]
  (loop [i low
         m (transient {})]
    (if (< i high)
      (recur (unchecked-add i 2)
             (assoc! m (aget captures i)
                       (aget captures (unchecked-inc i))))
      (persistent! m))))


(defn json-parser
  []
  (let [matchers {:digit19  (pex/single-range-matcher \1 \:)
                  :digit    (pex/single-range-matcher \0 \:)
                  :hexdigit (pex/range-matcher [[\a \g]
                                                [\0 \:]])}
        actions {:append-hexdigit (pex/update-stack-top identity)
                 :capture-object  (pex/replace-captures make-json-object)
                 :capture-array   (pex/fold-cap (fn
                                                  ([] (transient []))
                                                  ([res] (persistent! res))
                                                  ([res input] (conj! res input))))
                 :cast-number     pex/clear-sb              ;; fixme
                 :clear-sb        pex/clear-sb
                 :append-sb       pex/append-sb
                 :push-sb         pex/push-sb
                 :push-true       (pex/push true)
                 :push-false      (pex/push false)
                 :push-nil        (pex/push nil)}]
    (pex/compile JSON 'value matchers actions json-macros)))

(defn trial-run
  [peg input]
  (pex/matcher peg (.toCharArray input) (StringBuffer.)))

(def CSV '{file [OWS record (* NL record) EOI]

           record [field (* field-delimeter field)]

           field-delimeter ","
           field (/ quoted unquoted)

           unquoted (capture (* (class nonquotechars)))
           ;; (capture (not \") (* ANY))
           quoted [OWS \"
                   (capture (* (/ (class quotechars) "\"\"")))
                   ;; (apply unescape-quotes (* (/ (class quotechars)
                   ;;                              "\"\"")))
                   ;;

                   \" OWS
                   (action unescape-quotes)]

           NL [(? \r) \n]
           OWS (* (class :ws))})

(def csv-field '{record [field (* sep field) "\n" EOI]
                 field  (/ quoted unquoted)
                 quoted ["\"" [(not "\"") ANY]  ]
                 sep ","})

(def csv-macros {:ws   (fn [patt] [patt 'whitespace])
                 :join (fn [patt sep] [patt (list '* sep patt)])})
