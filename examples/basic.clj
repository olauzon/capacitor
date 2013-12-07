(require '[capacitor.core :as influx])

(def client
  (influx/make-client {:db "my-new-db"}))

(influx/create-db client)

(influx/delete-db client)

(influx/delete-db client)
;; => Exception

(influx/create-db client)

(influx/create-db-user client "myuser" "mypassword")

(def c
  (influx/make-client {
    :db       "my-new-db"
    :username "myuser"
    :password "mypassword" }))

(influx/post-points c "logins" [
  {:email "john@gmail.com"}
  {:email "john@yahoo.com"}
  {:email "john@hotmail.com"}
  {:email "jill@gmail.com"}
  {:email "jason@gmail.com"}
  {:email "alice@yahoo.com"}
  {:email "bob@mac.com"} ])

(def query-00
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(10m) "
    "WHERE email =~ /.*gmail\\.com/"))

(def query-01
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(1m)"))

(influx/get-query c query-00)

(influx/get-query c query-01)
