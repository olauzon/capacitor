(ns capacitor.core
  (:require
    [capacitor.http :as http :only
      [make-client]]
    [capacitor.status :as status :only
      [ping
       version]]
    [capacitor.db :as db :only
      [create-db
       list-dbs
       delete-db]]
    [capacitor.user :as user :only
      [create-db-user
       list-db-users
       update-user-password
       list-user-permissions
       grant-db-user-permissions
       revoke-db-user-permissions
       delete-user]]
    [capacitor.query :as query :only
      [db-query]]
    [capacitor.point :as point :only
      [->Tag
       ->Field
       ->Point
       ->tag
       ->field
       point
       ->point
       ->line
       ->lines]]
    [capacitor.write :as write :only
      [post]]
    [capacitor.series :as series :only
      [list-series]]))

(defn make-client
  "Returns a map representing an HTTP client configuration.

    Valid options:
      :host         (default: \"localhost\")
      :scheme       (default: \"http://\")
      :port         (default: 8086)
      :username     (default \"root\")
      :password     (default \"root\")
      :db           (default: \"testdb\")
      :version      (default: \"0.9\")
      :post-opts    (http post options, default: \"nil\")
      :get-opts     (http get options, default: \"nil\")"
  [opts]
  (http/make-client opts))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ## Database status

(defn version
  "Get database version"
  [client]
  (status/version client))

(defn ping
  "Get database status. Returns ping time in ms"
  [client]
  (status/ping client))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ## Database management

(defn create-db
  "Create database defined in client"
  [client]
  (db/create-db client))

(defn list-dbs
  "Returns list of database names"
  [client]
  (db/list-dbs client))

(defn delete-db
  "Delete database defined in client"
  [client]
  (db/delete-db client))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ## Database users

(defn create-db-user
  "Create new database user"
  ([client username password]
    (user/create-db-user client username password :all))
  ([client username password permissions]
    (user/create-db-user client username password permissions)))

(defn list-db-users
  "List database users"
  [client & [results->maps?]]
  (user/list-db-users client results->maps?))

(defn update-user-password
  "Update database user password"
  [client username password]
  (user/update-user-password client username password))

(defn list-user-permissions
  "Show database user privileges"
  [client username & [results->maps?]]
  (user/list-user-permissions client username results->maps?))

(defn grant-db-user-permissions
  "Grant database user privileges"
  [client username permissions]
  (user/grant-db-user-permissions client username permissions))

(defn revoke-db-user-permissions
  "Revoke database user privileges"
  [client username permissions]
  (user/revoke-db-user-permissions client username permissions))

(defn delete-user
  "Delete user"
  [client username]
  (user/delete-user client username))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ## Queries

(defn db-query
  "Perform arbitrary query string"
  ([client q & [results->maps?]]
    (query/db-query client q results->maps?)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ## Points

(defn ->tag
  "Make a Tag record, optionally from a vector, a map, or a Tag"
  ([k v]
    (point/->Tag k v))
  ([x]
    (point/->tag x)))

(defn ->field
  "Make a Field record, optionally from a vector, a map, or a Field"
  ([k v]
    (point/->Field k v))
  ([x]
    (point/->field x)))

(defn ->timestamp
  "Make a Timestamp record, from a Long, a String, or a Timestamp"
  [x]
  (point/->timestamp x))

(defn ->point
  "Make a Point record"
  ([measurement tags fields timestamp]
    (point/point measurement tags fields timestamp))
  ([x]
    (point/->point x)))

(defn ->line
  "Dump Point record to line"
  [p]
  (point/->line p))

(defn ->lines
  "Dump collection of Point records to lines"
  [ps]
  (point/->lines ps))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ## Write

(defn write-point
  "Write Point record to database defined in client"
  [client point]
  (write/post client (-> point ->point ->line)))

(defn write-points
  "Write collection of Point records to database defined in client"
  [client points]
  (write/post client (->> points (map ->point) vec ->lines)))

(defn write-lines
  "Write using raw line String"
  [client lines]
  (write/post client lines))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ## Series

(defn list-series
  "List series"
  [client & [results->maps?]]
  (series/list-series client results->maps?))
