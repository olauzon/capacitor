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
  :email  "paul@gmail.com" })

(influx-async/enqueue events-in {
  :series "signups"
  :email  "john@gmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "ringo@gmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "george@gmail.com" })

(influx-async/enqueue events-in {
  :series "signups"
  :email  "syd@hotmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "roger@hotmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "nick@hotmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "rick@hotmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "david@hotmail.com" })

(influx-async/enqueue events-in {
  :series "signups"
  :email  "sting@yahoo.com" })

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

(influx-async/get-query c query-00 results-out)

(influx-async/get-query c query-00 results-out)
