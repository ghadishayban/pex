(ns com.champbacon.json
  (:require [com.champbacon.pex :as pex]))

(def JSON '{json    [whitespace value EOI]

            value (/ string number object array jtrue jfalse jnull)

            object  [(:ws "{")
                     (:join [string (:ws ":") value] ",")
                     (:ws "}")
                     (action capture-object)]

            array [(:ws "[") (:join value (:ws ",")) (:ws "]")]

            string [\" (action clear-sb) characters (:ws \") (action push-sb)]

            number (:ws (capture integer (? frac) (? exp)))

            characters (/ (* normal-character)
                          [\\ escaped-character])

            quote-backslash "\"\\"
            normal-character [(not quote-backslash) any (action append-last)]
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
            jtrue  ["true" whitespace]
            jfalse ["false" whitespace]
            jnull  ["null" whitespace]
            whitespace (* (:anyof " \n\r\t\f"))})

;; case(syms...)  tail recursion

(def json-macros {:anyof       (fn [str] (apply list '/ (seq str)))
                  :ws          (fn [patt] [patt 'whitespace])
                  :join        (fn [patt sep] [patt (list '* sep patt)])
                  :ignore-case identity})

#_(compile JSON 'value
         {:digit19 even?
          :digit even?
          :hexdigit even?}
         {:append-hexdigit even?
          :capture-object  even?
          :clear-sb        even?
          :append-sb       even?
          :push-sb         even?
          :append-last     even?}
         json-macros)


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
