(require '[capacitor.core :as influx])

(def client (influx/make-client {:db "my-new-db"}))

(influx/create-db client)

(influx/create-shard-space client {:name "example" :regex "/.*/" :retentionPolicy "1d" :shardDuration "1h" :replicationFactor 1 :split 1})

(influx/get-db-users client)

(influx/create-db-user client "myuser" "mypassword")

(influx/get-db-users client)

(influx/create-db-user client "myuser2" "mypassword2")

(influx/get-db-users client)

(influx/delete-db-user client "myuser2")

(influx/get-db-users client)

(influx/create-db-user client "myuser2" "mypassword2")

(influx/update-db-user client "myuser2" { :password "mypassword42" })

(influx/create-admin-user client "newuser" "newpassword")

(influx/update-admin-user client "newuser" {:password "password"})

(influx/get-admin-users client)

(influx/delete-admin-user client "newuser")
