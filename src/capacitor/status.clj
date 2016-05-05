(ns capacitor.status
  (:require [capacitor.http :refer [execute]]))

(defn ping-req
  [client]
  (execute client :ping ""))

(defn ping
  [client]
  (let [start (. System (nanoTime))]
    (ping-req client)
    (/ (double (- (. System (nanoTime)) start)) 1000000.0)))

(defn version
  [client]
  (((ping-req client) :headers) "X-Influxdb-Version"))
