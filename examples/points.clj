(use 'capacitor.core)

(def c (make-client {:db "mynewdb" :username "myuser" :password "mypassword"}))

;; `write-point`(s) can accept points in a variety of formats:

;; map
(write-point c
  {:measurement "cpu_load"
   :tags        {"host" "1" "dc" 1}
   :fields      {"value" 1.1}
   :timestamp   1457624412})

;; vector
(write-point c
;; measurement tags                fields        timestamp
  ["cpu_load"  {"host" "1" "dc" 1} {"value" 1.2} 1457624413])

;; map of vectors
(write-point c
  {:measurement "cpu_load"
   :tags        [["host" "1"] ["dc" 3]]
   :fields      [["value" 1.0] ["value_int" 1] ["web" "app"]]
   :timestamp   1457624413})

;; vector of vectors
(write-point c
  ["cpu_load"                                    ;; measurement
   [(list "host" "1") ["dc" 3]]                  ;; tags
   [["value" 1.0] ["value_int" 1] ["web" "app"]] ;; fields
   1457624413])                                  ;; timestamp
