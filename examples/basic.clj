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

(influx/post-points c "logins-00" [
  {:user "john"}
  {:user "john"}
  {:user "john"}
  {:user "jill"}
  {:user "jason"}
  {:user "alice"}
  {:user "bob"} ])

(influx/post-points c "posts-01" [
  {:user "charlie"}
  {:user "miles"}
  {:user "bill"}
  {:user "ella"}
  {:user "wayne"}
  {:user "joe"}
  {:user "cannonball"} ])

(def query
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(1s)"))

(influx/get-query c query)

(influx/delete-series c "logins")

(influx/list-series c)
