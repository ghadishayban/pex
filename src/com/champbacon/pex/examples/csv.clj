(ns com.champbacon.pex.examples.csv
  (:require [com.champbacon.pex :as pex]))

;; THIS IS INCOMPLETE AND INCORRECT

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

(def csv-field '{record [field (* sep field) EOI]
                 field  (/ quoted unquoted)
                 quoted ["\"" [(not "\"") ANY]  ]
                 sep ","})

(def csv-macros {:ws   (fn [patt] [patt 'whitespace])
                 :join (fn [patt sep] [patt (list '* sep patt)])})