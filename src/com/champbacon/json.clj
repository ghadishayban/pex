(ns com.champbacon.json
  (:require [com.champbacon.pex :as pex]))

(def JSON '{json    [whitespace value EOI]

            value (/ string
                     number
                     object
                     array
                     jtrue
                     jfalse
                     jnull)

            object  [(:ws "{") (:join pair ",") (:ws "}")]
            pair    [string (:ws ":") value]

            array [(:ws "[") (:join value (:ws ",")) (:ws "]")]

            string [\" characters (:ws \")]

            number [(capture integer (? frac) (? exp)) whitespace]

            characters (/ (* normal-character)
                          ["\\" escaped-character])

            quote-backslash "\"\\"
            normal-character [(not quote-backslash) any]
            escaped-character (/ quote-backslash
                                 "b"
                                 "f"
                                 "n"
                                 "r"
                                 "t"
                                 unicode)
            unicode ["u" (capture (class hexdigit)
                                  (class hexdigit)
                                  (class hexdigit)
                                  (class hexdigit))]
            integer [(? "-") (/ [(class digit19) digits]
                                (class digit))]
            digits  [(class digit) (* (class digit))]
            frac    ["." digits]
            exp     [(:ignore-case "e") (? (:anyof "+-")) digits]
            jtrue  ["true" whitespace]
            jfalse ["false" whitespace]
            jnull  ["null" whitespace]
            whitespace (* (:anyof " \n\r\t\f"))})

(def json-macros {:anyof       (fn [str] (apply list '/ (seq str)))
                  :ws          (fn [patt] [patt 'whitespace])
                  :join        (fn [patt sep] [patt (list '* sep patt)])
                  :ignore-case identity})

;; (run! prn (pex/compile JSON 'json json-macros))

(def CSV '{
           file [OWS record (* NL record) EOI]
           record [field (* field-delimeter field)]

           field-delimeter ","
           field (/ quoted unquoted)

           unquoted (capture (* (class nonquotechars)))
           ;; (capture (not \") (* ANY))
           quoted [OWS \"
                   (capture (/ (class quotechars) "\"\""))
                   \" OWS]

           NL [(? \r) \n]
           OWS (* (class :ws))})

(def csv-macros {:ws   (fn [patt] [patt 'whitespace])
                 :join (fn [patt sep] [patt (list '* sep patt)])})
