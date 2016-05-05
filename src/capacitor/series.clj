(ns capacitor.series
  (:require [capacitor.http :refer [execute extract]]))

(defn list-series-req
  [client]
  (execute client :list-series "SHOW SERIES"))

(defn list-series
  [client results->maps?]
  (-> (list-series-req client) (extract results->maps?)))
