(ns capacitor.query
  (:require [capacitor.http :refer [execute extract]]))

(defn db-query-req
  [client q]
  (execute client :db-query q))

(defn db-query
  [client q results->maps?]
  (-> (db-query-req client q) (extract results->maps?)))
