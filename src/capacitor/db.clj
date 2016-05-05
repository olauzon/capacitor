(ns capacitor.db
  (:require [capacitor.http :refer [execute]]))

(defn create-db-req
  "Create database defined in client."
  [client]
  (execute client :create-db (str "CREATE DATABASE " (client :db))))

(defn create-db
  "Create database defined in client."
  [client]
  (= 200 ((create-db-req client) :status)))

(defn list-dbs-req
  "List databases. Returns raw HTTP response."
  [client]
  (execute client :get-dbs "SHOW DATABASES"))

(defn list-dbs
  "Returns list of database names."
  [client]
  (-> (list-dbs-req client)
      :body :results first :series first :values flatten))

(defn delete-db-req
  "Delete database defined in client."
  [client]
  (execute client :delete-db (str "DROP DATABASE " (client :db))))

(defn delete-db
  "Delete database defined in client."
  [client]
  (= 200 ((delete-db-req client) :status)))
