(use '[capacitor.core])
(use '[capacitor.async])

;; Define an InfluxDB client (see basic.clj for example)
(def c
  (make-client {
    :db       "my-new-db"
    :username "myuser"
    :password "mypassword" }))

;; Make a channel to buffer incoming events
(def events-in (atom (make-chan)))

;; Make a channel to collect post responses
(def resp-out (atom (make-chan)))

;; Start the run loop with a batch size of max 10 events and max 5 seconds
(run! @events-in @resp-out c 10 5000)

;; Enqueue events
(enqueue @events-in {:email "paul@gmail.com"
                     :series "logins"})

(enqueue @events-in {:email "john@gmail.com"
                     :series "signups"})

(enqueue @events-in {:email "ringo@gmail.com"
                     :series "logins"})

(enqueue @events-in {:email "george@gmail.com"
                     :series "logins"})

(enqueue @events-in {:email "syd@hotmail.com"
                     :series "signups"})

(enqueue @events-in {:email "roger@hotmail.com"
                     :series "logins"})

(enqueue @events-in {:email "nick@hotmail.com"
                     :series "logins"})

(enqueue @events-in {:email "rick@hotmail.com"
                     :series "logins"})

(enqueue @events-in {:email "david@hotmail.com"
                     :series "logins"})

(enqueue @events-in {:email "sting@yahoo.com"
                     :series "signups"})

(def query-00
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(1s)"))

(get-query c query-00)
