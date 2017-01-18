(require '[capacitor.influxdb09.core :as influx])

(def client
  (influx/make-client {:db "testdb"}))

(influx/post-points client "cpu,host=webserver01,dc=ny,type=idle value=80")
