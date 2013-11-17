(ns my.app
  (:use [capacitor.core]))

(def client
  (make-client {:db "my-new-db"}))

(create-db client)

(delete-db client)

(delete-db client)
;; => Exception

(create-db client)

(create-db-user client "myuser" "mypassword")

(def c
  (make-client {
    :db       "my-new-db"
    :username "myuser"
    :password "mypassword" }))

(post-points c "logins"
  [
    {:email "john@gmail.com"}
    {:email "john@yahoo.com"}
    {:email "john@hotmail.com"}
    {:email "jill@gmail.com"}
    {:email "jason@gmail.com"}
    {:email "alice@yahoo.com"}
    {:email "bob@mac.com"} ])

(def query-01
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(10m) "
    "WHERE email =~ /.*gmail\\.com/"))

(def query-02
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(1m)"))

(get-query c query-01)

(get-query c query-02)
