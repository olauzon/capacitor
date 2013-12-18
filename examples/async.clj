;; Base InfluxDB library
(require '[capacitor.core :as influx])

;; Async API
(require '[capacitor.async :as influx-async])

;; Require core.async
(require '[clojure.core.async :as async])

;; Define an InfluxDB client (see basic.clj for example)
(def c
  (influx/make-client {
    :db       "my-new-db"
    :username "myuser"
    :password "mypassword" }))

;; Make a channel to buffer incoming events
(def events-in (influx-async/make-chan))

;; Make a channel to collect post responses
(def resp-out (influx-async/make-chan))

;; Start the run loop with a batch size of max 10 events and max 5 seconds
(influx-async/run! events-in resp-out c 10 5000)

;; Enqueue events
(influx-async/enqueue events-in {
  :series "logins"
  :email  "paulgmail.com" })

(influx-async/enqueue events-in {
  :series "signups"
  :email  "johngmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "ringogmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "georgegmail.com" })

(influx-async/enqueue events-in {
  :series "signups"
  :email  "sydhotmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "rogerhotmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "nickhotmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "rickhotmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "davidhotmail.com" })

(influx-async/enqueue events-in {
  :series "signups"
  :email  "stingyahoo.com" })

(dotimes [i 12]
  (influx-async/enqueue events-in {
    :series "logins"
    :email  (str i "@example.com") }))

(def query-00
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(1s)"))

(def query-01
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(1m)"))

(influx/get-query c query-00)


;; Close the `run!` loop
(async/close! events-in)

;; Async queries

;; Make a channel to collect query results
(def results-out (influx-async/make-chan))

(influx-async/get-query c query-00 results-out)

(influx-async/get-query c query-01 results-out)

(influx-async/read-results results-out)


(async/go
  (loop [i 0]
  (when-let [r (async/<! results-out)]
    (println (str "result " i ": "))
    (println r)
    (recur (inc i)))))
