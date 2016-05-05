(ns capacitor.user
  (:require [clojure.string :refer [upper-case]]
            [capacitor.http :refer [execute extract]]))

(defn create-db-user-req
  [client username password permissions]
  (execute client :create-db-user
    (str
      "CREATE USER " username " WITH PASSWORD '" password "';"
      "GRANT " (-> permissions name upper-case)
      " ON " (client :db) " TO " username ";")))

(defn create-db-user
  [client username password permissions]
  (= 200
    ((create-db-user-req client username password permissions) :status)))

(defn list-db-users-req
  [client]
  (execute client :list-db-user-users "SHOW USERS"))

(defn list-db-users
  [client results->maps?]
  (-> (list-db-users-req client)
      (extract results->maps?)))

(defn update-user-password-req
  [client username password]
  (execute client :update-db-user-password
    (str "SET PASSWORD FOR " username " = '" password "'")))

(defn update-user-password
  "Update database user password"
  [client username password]
  (= 200
     ((update-user-password-req client username password) :status)))

(defn list-user-permissions-req
  [client username]
  (execute client :grant-db-user-permissions
    (str "SHOW GRANTS FOR " username)))

(defn list-user-permissions
  [client username results->maps?]
  (-> (list-user-permissions-req client username)
      (extract results->maps?)))

(defn grant-db-user-permissions-req
  [client username permissions]
  (execute client :grant-db-user-permissions
    (str "GRANT " (-> permissions name upper-case)
         " ON " (client :db) " TO " username ";")))

(defn grant-db-user-permissions
  [client username permissions]
  (= 200
     ((grant-db-user-permissions-req client username permissions) :status)))

(defn revoke-db-user-permissions-req
  [client username permissions]
  (execute client :revoke-db-user-permissions
    (str "REVOKE " (-> permissions name upper-case)
         " ON " (client :db) " FROM " username ";")))

(defn revoke-db-user-permissions
  [client username permissions]
  (= 200
     ((revoke-db-user-permissions-req client username permissions) :status)))

(defn delete-user-req
  [client username]
  (execute client :delete-user (str "DROP USER " username)))

(defn delete-user
  [client username]
  (= 200
     ((delete-user-req client username) :status)))
