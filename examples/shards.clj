(require '[capacitor.core :as influx])

(def client (influx/make-client {:db "my-new-db"}))

(influx/get-shards client)

(influx/get-shard-spaces client)

(influx/drop-shard-space client "test" "default")

(influx/drop-shard client 4 1)

