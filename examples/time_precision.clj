(require '[clojure.pprint :refer [pprint]])
(require '[capacitor.core :as influx])

(def client
  (influx/make-client {:db "my-new-db"}))

(influx/create-db client)

(influx/create-db-user client "myuser" "mypassword")

(def c
  (influx/make-client {
    :db       "my-new-db"
    :username "myuser"
    :password "mypassword" }))

(influx/post-points c "events" "u" [
  {:email "john@example.com"
   :time  1392999320123456 }
  {:email "john@example.com"
   :time  1389502800296182 }
  {:email "john@example.com"
   :time  1390021200412921 }
  {:email "john@example.com"
   :time  1392138671021823 }
  {:email "john@example.com"
   :time  1392300163201380 } ])

(pprint (influx/get-query c "u" "SELECT * FROM events"))
;; => returns points in microseconds

(pprint (influx/get-query c "m" "SELECT * FROM events"))
;; => returns points in milliseconds

(pprint (influx/get-query c "s" "SELECT * FROM events"))
;; => returns points in seconds

(pprint (influx/get-query c "SELECT * FROM events"))
;; => returna points in milliseconds
