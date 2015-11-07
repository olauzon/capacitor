(ns capacitor.core
  (:use [clojure.string :only [split]])
  (:require [clj-http.client :as http-client]
            [clojure.set :as set]
            [cheshire.core   :as json])
  (import [java.net URLEncoder]))

;;
;; ## Client options
;;

(def default-client
  "Default HTTP client configuration"
   { :host        "localhost"
     :scheme      "http"
     :port        8086
     :username    "root"
     :password    "root"
     :db          "testdb"
     :version     "0.8" })

;;
;; ## Default write policy options
;;

(def default-write-options
  "Default write policy options"
  { :retention-policy   "default"
    :precision          "s"
    :consistency        "all" })

(defn make-client
  "Returns a map representing an HTTP client configuration.

    Valid options:
      :host         (default: \"localhost\")
      :scheme       (default: \"http://\")
      :port         (default: 8086)
      :username     (default \"root\")
      :password     (default \"root\")
      :db           (default: \"default-db\")
      :post-opts    (http post options, default: \"nil\")
      :get-opts     (http get options, default: \"nil\")"
  [opts]
  (merge default-client opts))

;;
;; ## HTTP URL generation for InfluxDB version >= 0.9
;;

(defn gen-url-9-fn
  {:no-doc true}
  [client action]
  (str
   (client :scheme)
   "://"
   (client :host)
   ":"
   (client :port)
   (str
     (cond
       (= (action :action) :post-points) (str "/write?"
                                              "db=" (client :db) "&"
                                              "rp=" (action :retention-policy) "&"
                                              "precision=" (action :precision) "&"
                                              "consistency=" (action :consistency) "&"
                                              "u=" (client :username) "&"
                                              "p=" (client :password))
       (or (= (action :action) :get-query)
           (= (action :action) :get-series)) (str "/query?"
                                                  "db=" (client :db) "&"
                                                  "u=" (client :username) "&"
                                                  "p=" (client :password) "&"
                                                  "q=")
       :else (str "/query?"
                  "u=" (client :username) "&"
                  "p=" (client :password) "&"
                  "q=")))))

;;
;; ## HTTP URL generation for InfluxDB version < 0.9
;;

(defn gen-url-8-fn
  {:no-doc true}
  [client action]
  (str
    (client :scheme)
    "://"
    (client :host)
    ":"
    (client :port)
    (cond (contains? #{:ping} (action :action))
          (str "/" (-> action :action name))
          (contains? #{:sync} (action :action))
          (str "/"
               (-> action :action name)
               "?u="
               (client :username)
               "&p="
               (client :password))
          (contains? #{ :create-admin-user
                        :delete-admin-user
                        :update-admin-user
                        :get-admin-users } (action :action))
          (str
          "/cluster_admins"
          (cond
            (contains? #{ :update-admin-user
                          :delete-admin-user } (action :action))
              (str "/" (action :username)))
          "?u="
          (client :username)
          "&p="
          (client :password))
          (contains? #{ :drop-shard
                        :get-shards
                        :get-shard-spaces
                        :update-shard-space
                        :drop-shard-space
                        :create-shard-space } (action :action))
          (str
           "/cluster"
           (cond
             (= (action :action) :get-shard-spaces) "/shard_spaces"
             (= (action :action) :update-shard-space) (str "/shard_spaces/" (client :db) "/" (client :shard-space))
             (= (action :action) :drop-shard-space) (str "/shard_spaces/" (client :db) "/" (client :shard-space))
             (= (action :action) :create-shard-space) (str "/shard_spaces/" (client :db))
             (= (action :action) :get-shards) "/shards"
             (= (action :action) :drop-shard) (str "/shards/" (client :shard-id)))
           "?u="
           (client :username)
           "&p="
           (client :password))
    :else
    (str
    "/db"
    (cond
      (= (action :action) :delete-series) (str "/" (client :db) "/series/" (action :series))
      (= (action :action) :delete-db) (str "/" (client :db))
      (contains? #{ :create-db-user
                    :get-db-user-users
                    :update-db-user
                    :delete-db-user } (action :action))
        (str "/" (client :db) "/users")
      (or (= (action :action) :post-points) (= (action :action) :get-query))
          (str "/" (client :db) "/series"))
    (cond
      (contains? #{ :update-db-user
                    :delete-db-user } (action :action))
        (str "/" (action :username)))
    "?u="
    (client :username)
    "&p="
    (client :password)
    (when (action :time-precision)
      (str "&time_precision=" (action :time-precision)))
    (cond
      (= (action :action) :get-query) "&q=")))))

(defn gen-url-fn
  {:no-doc true}
  [client action]
  (case (:version client)
    "0.8" (gen-url-8-fn client action)
    "0.9" (gen-url-9-fn client action)))

(defmulti gen-url-multi
  (fn [_ action] (class action)))

(defmethod gen-url-multi clojure.lang.Keyword
  [client action]
  (gen-url-fn client { :action action }))

(defmethod gen-url-multi clojure.lang.PersistentArrayMap
  [client action]
  (gen-url-fn client action))

(def gen-url
  (memoize gen-url-multi))

(defn- kw-parse-string
  "Parse the JSON string, coerce keys to keywords"
  [string]
  (json/parse-string string true))

;;
;; ## Database status
;;

(defn ping
  [client]
  (-> client
      (gen-url :ping)
      (http-client/get
        (merge {:socket-timeout        1000
                :conn-timeout          1000
                :accept                :json
                :throw-entire-message? true}
               (:get-opts client)))
      :body
      kw-parse-string))

;;
;; ## InfluxDB Version
;;

(defn get-version
  [array]
  (if (= (count array) 1) (first array) (second array)))

(defn version
  [client]
  (-> (-> client
        (gen-url :ping)
        (http-client/get
          (merge {:socket-timeout        10000
                  :conn-timeout          10000
                  :accept                :json
                  :throw-entire-message? true}
                  (:get-opts client))))
      (:headers)
      (get "x-influxdb-version")
      (split #"\s")
      (get-version)))

(defn sync?
  [client]
  (-> client
      (gen-url :sync)
      (http-client/get
        (merge {:socket-timeout        1000
                :conn-timeout          1000
                :accept                :json
                :throw-entire-message? true}
               (:get-opts client)))
      :body
      Boolean/parseBoolean))

;;
;; ## Database management
;;

;;;;;;;;;;;;;;;;;;;;;;;;
;; ### Create a database

(defn create-db-req-8
  "Create database defined in client. Returns raw HTTP response."
  [client]
  (let [url  (gen-url client :create-db)
        body (json/generate-string { :name (client :db) })]
    (http-client/post url {
      :body                 body
      :socket-timeout       1000 ;; in milliseconds
      :conn-timeout         1000 ;; in milliseconds
      :content-type         :json
      :throw-entire-message? true })))

(defn create-db-req-9
  "Create database defined in client. Returns raw HTTP response."
  [client]
  (let [url (gen-url client :create-db)
        uri (URLEncoder/encode (str "CREATE DATABASE " (client :db)))]
    (http-client/get (str url uri) {
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :accept                :json
      :throw-entire-message? true })))

(defn create-db-req
  [client]
  (case (:version client)
    "0.8" (create-db-req-8 client)
    "0.9" (create-db-req-9 client)))

(defn create-db
  "Create database defined in client. Returns HTTP status on success."
  [client]
  ((create-db-req client) :status))

;;;;;;;;;;;;;;;;;;;;;
;; ### List databases

(defn get-dbs-req-8
  "List databases. Returns raw HTTP response."
  [client]
  (let [url (gen-url client :get-dbs)]
    (http-client/get url {
      :socket-timeout        1000  ;; in milliseconds
      :conn-timeout          1000  ;; in milliseconds
      :accept                :json
      :throw-entire-message? true })))

(defn get-dbs-req-9
  "List databases. Returns raw HTTP response."
  [client]
  (let [url (gen-url client :get-dbs)
        uri (URLEncoder/encode "SHOW DATABASES")]
    (http-client/get (str url uri) {
      :socket-timeout        1000  ;; in milliseconds
      :conn-timeout          1000  ;; in milliseconds
      :accept                :json
      :throw-entire-message? true })))

(defn get-dbs-req
  [client]
  (case (:version client)
    "0.8" (get-dbs-req-8 client)
    "0.9" (get-dbs-req-9 client)))

(defn get-dbs-8
  "Returns vector of database names."
  [client]
  (json/parse-string ((get-dbs-req client) :body) true))

(defn get-dbs-9
  "Returns vector of database names."
  [client]
  (mapcat
   (fn [{:strs [columns values]}]
     (map (partial zipmap (map keyword columns)) values))
   (flatten (map #(get % "series") (get (json/parse-string ((get-dbs-req client) :body)) "results")))))

(defn get-dbs
  [client]
  (case (:version client)
    "0.8" (get-dbs-8 client)
    "0.9" (get-dbs-9 client)))

;;;;;;;;;;;;;;;;;;;;;;
;; ### Drop a database

(defn delete-db-req-8
  "Delete database defined in client. Returns raw HTTP response."
  [client]
  (let [url (gen-url client :delete-db)]
    (http-client/delete url {
      :socket-timeout        10000 ;; in milliseconds
      :conn-timeout          10000 ;; in milliseconds
      :throw-entire-message? true })))

(defn delete-db-req-9
  "Delete database defined in client. Returns raw HTTP response."
  [client]
  (let [url (gen-url client :delete-db)
        uri (URLEncoder/encode (str "DROP DATABASE " (client :db)))]
    (http-client/get (str url uri) {
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :accept                :json
      :throw-entire-message? true })))

(defn delete-db-req
  [client]
  (case (:version client)
    "0.8" (delete-db-req-8 client)
    "0.9" (delete-db-req-9 client)))

(defn delete-db
  "Delete database defined in client. Returns HTTP status on success."
  [client]
  ((delete-db-req client) :status))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ### Delete a series of a database

(defn delete-series-req
  "Delete a series of a database. Returns raw HTTP response."
  [client series]
  (let [url (str (gen-url client {:action :delete-series
                                  :series series}))]
    (http-client/delete url {
      :socket-timeout        10000 ;; in milliseconds
      :conn-timeout          10000 ;; in milliseconds
      :throw-entire-message? true })))

(defn delete-series
  "Delete a series of a database. Returns HTTP status on success."
  [client series]
  ((delete-series-req client series) :status))

;;
;; ## User management
;;

;; ### Cluster admins

;; #### Create admin user

(defn create-admin-user-req
  "Create new admin user. Returns full HTTP response."
  [client username password]
  (let [url  (gen-url client :create-admin-user)
        body (json/generate-string {
               :name     username
               :password password })]
    (http-client/post url {
      :body                  body
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :content-type          :json
      :throw-entire-message? true })))

(defn create-admin-user
  "Create new admin user. Returns HTTP status on success."
  [client username password]
  ((create-admin-user-req client username password) :status))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### List admin users

(defn get-admin-users-req
  [client]
  (let [url  (gen-url client :get-admin-users)]
    (http-client/get url {
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :content-type          :json
      :throw-entire-message  true })))

(defn get-admin-users
  "List admin users"
  [client]
  (json/parse-string ((get-admin-users-req client) :body) true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### Update admin user

(defn update-admin-user-req
  [client username options]
  (let [url  (gen-url client { :action :update-admin-user
                               :username username })
        attrs (merge {:name username} options)
        body  (json/generate-string attrs)]
    (http-client/post url {
      :body                  body
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :content-type          :json
      :throw-entire-message? true })))

(defn update-admin-user
  [client username options]
  ((update-admin-user-req client username options) :status))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### Delete admin user

(defn delete-admin-user-req
  "Delete an admin user. Returns raw HTTP response."
  [client username]
  (let [url (gen-url client { :action :delete-admin-user
                              :username username })]
    (http-client/delete url {
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :throw-entire-message? true })))

(defn delete-admin-user
  "Delete an admin user. Returns HTTP status on success."
  [client username]
  ((delete-admin-user-req client username) :status))


;; ### Database users

;; #### Create database user

(defn create-db-user-req
  "Create new database user. Returns full HTTP response."
  [client username password]
  (let [url  (gen-url client :create-db-user)
        body (json/generate-string {
               :name     username
               :password password })]
    (http-client/post url {
      :body                  body
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :content-type          :json
      :throw-entire-message? true })))

(defn create-db-user
  "Create new database user. Returns HTTP status on success."
  [client username password]
  ((create-db-user-req client username password) :status))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### List database users

(defn get-db-users-req
  [client]
  (let [url  (gen-url client :get-db-user-users)]
    (http-client/get url {
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :content-type          :json
      :throw-entire-message? true })))

(defn get-db-users
  "List database users"
  [client]
  (json/parse-string ((get-db-users-req client) :body) true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### Update database user

(defn update-db-user-req
  [client username options]
  (let [url  (gen-url client { :action :update-db-user
                               :username username })
        attrs (merge {:name username} options)
        body  (json/generate-string attrs)]
    (http-client/post url {
      :body                  body
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :content-type          :json
      :throw-entire-message? true })))

(defn update-db-user
  [client username options]
  ((update-db-user-req client username options) :status))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### Delete database user

(defn delete-db-user-req
  "Delete database defined in client. Returns raw HTTP response."
  [client username]
  (let [url (gen-url client { :action  :delete-db-user
                              :username username })]
    (http-client/delete url {
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :throw-entire-message? true })))

(defn delete-db-user
  "Delete database defined in client. Returns HTTP status on success."
  [client username]
  ((delete-db-user-req client username) :status))

;;
;; ## Shards and Shards spaces (From v0.8.0 onwards)
;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### Get all shards

(defn get-shards-req
  [client]
  (let [url  (gen-url client :get-shards)]
    (http-client/get url {
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :content-type          :json
      :throw-entire-message? true })))

(defn get-shards
  "List shards."
  [client]
  (json/parse-string ((get-shards-req client) :body) true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### Shard key mappings functions

(def ^{:private true} api->shard-config {:retentionPolicy   :retention-policy
                                         :shardDuration     :shard-duration
                                         :replicationFactor :replication-factor})
(def ^{:private true} shard-config->api (set/map-invert api->shard-config))

(defn api->shard-config-keys
  [l]
  (map #(set/rename-keys % api->shard-config) l))

(defn shard-config->api-keys
  [m]
  (set/rename-keys m shard-config->api))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### Get all shard spaces

(defn get-shard-spaces-req
  [client]
  (let [url  (gen-url client :get-shard-spaces)]
    (http-client/get url {
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :content-type          :json
      :throw-entire-message? true })))

(defn get-shard-spaces
  "List shard spaces."
  [client]
  (-> client
      get-shard-spaces-req
      :body
      (json/parse-string true)
      api->shard-config-keys))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### Create shard space

(defn create-shard-space-req
  "Create shard space. Returns full HTTP response.
  Default parameters: regex \"/.*/\", retention-policy \"inf\", shard-duration \"7d\""
  [client {:keys [name regex retention-policy shard-duration replication-factor split]
           :or {regex "/.*/"
                retention-policy "inf"
                shard-duration "7d"}}]
  (let [url  (gen-url client :create-shard-space)
        body (json/generate-string {:name name
                                    :regex regex
                                    :retentionPolicy retention-policy
                                    :shardDuration shard-duration
                                    :replicationFactor replication-factor
                                    :split split})]
    (http-client/post url {
      :body                  body
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :content-type          :json
      :throw-entire-message? true })))

(defn create-shard-space
  "Create a new shard space in the current database."
  [client shard-config]
  (json/parse-string ((create-shard-space-req client shard-config) :body)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### Update shard space

(defn update-shard-space-req
  "Update shard space. Returns full HTTP response."
  [client database shard-space shard-config]
  (let [client (merge client { :shard-space shard-space
                               :db          database })
        url  (gen-url client :update-shard-space)
        body (-> shard-config shard-config->api-keys json/generate-string)]
    (http-client/post url {
                           :body                  body
                           :socket-timeout        1000 ;; in milliseconds
                           :conn-timeout          1000 ;; in milliseconds
                           :content-type          :json
                           :throw-entire-message? true })))

(defn update-shard-space
  "Update an existing shard space in the given database. All shard config parameters are required!"
  [client database shard-space {:keys [regex retention-policy shard-duration replication-factor split]
                                :as shard-config}]
  (json/parse-string ((update-shard-space-req client database shard-space shard-config) :body)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### Drop shard space

(defn drop-shard-space-req
  [client database shard-space]
  (let [client (merge client { :shard-space shard-space
                               :db          database })
        url (gen-url client :drop-shard-space)]
    (http-client/delete url {
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :throw-entire-message? true })))

(defn drop-shard-space
  "Drop shard space."
  [client database shard-space]
  ((drop-shard-space-req client database shard-space) :status))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #### Drop shard

(defn drop-shard-req
  [client shard-id server-id]
  (let [client (merge client { :shard-id shard-id })
        url (gen-url client :drop-shard)
        body (json/generate-string { :serverIds (vector server-id)})]
    (http-client/delete url {
      :body                  body
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :content-type          :json
      :throw-entire-message? true })))

(defn drop-shard
  "Drop shard."
  [client shard-id server-id]
  ((drop-shard-req client shard-id server-id) :status))

;;
;; ## Post time-series points for InfluxDB version < 0.9
;;

(defn post-points-8-req
  ([client points]
    (post-points-8-req client points nil))
  ([client points time-precision]
    (let [url  (gen-url client { :action         :post-points
                                 :time-precision time-precision })
          body (json/generate-string points)]
      (http-client/post url (merge {
        :body                  body
        :socket-timeout        1000 ;; in milliseconds
        :conn-timeout          1000 ;; in milliseconds
        :content-type          :json
        :throw-entire-message? true }
        (:post-opts client))))))

(defn format-payload
  [res]
  (let [res-seq (seq res)
        seq-cnt (dec (count res-seq))]
  (loop [curr-res []
         i 0]
    (let [rec (nth res-seq i)
          series-name (first rec)
          values  (last rec)
          data    (flatten (into [] [values]))
          columns (distinct (flatten (map keys data)))
          points  (map (fn [d] (map #(d %) columns)) data)
          new-res (conj curr-res
                        [{ :name    series-name
                           :points  points
                           :columns columns }])]
      (if (>= i seq-cnt)
        (vec (flatten new-res))
        (recur new-res (inc i)))))))

(defn make-payload
  "Returns normalized columns to post points to database"
  ([values]
    (let [data   (flatten (into [] [values]))
          number (dec (count data))]
      (loop [i   0
             res {}]
        (let [d           (nth data i)
              series      (merge {(d :series) []} res)
              series-data (conj (series (d :series))
                                (dissoc d :series))
              new-res     (assoc series (d :series) series-data)]
          (if (>= i number)
            (format-payload new-res)
            (recur (inc i) new-res))))))
  ([series-name values]
    (make-payload (map #(assoc % :series series-name) values))))

(defn post-points-8
  "Post points to database. Returns HTTP status on success.
  Points should be submitted as a vector of maps."
  ([client series-name values]
    ((post-points-8-req client (make-payload series-name values)) :status))
  ([client series-name time-precision values]
    ((post-points-8-req client
                        (make-payload series-name values)
                        time-precision) :status)))

;;
;; ## Post time-series points for InfluxDB version >= 0.9
;;

(defn post-points-9
  "Post points to database. Returns HTTP status on success.
   Uses influxdb's http line protocol to submit data-points"
  ([client points]
    (post-points-9 client points { :precision        nil
                                   :consistency      nil
                                   :retention-policy nil }))
  ([client points write-options]
    (let [url  (gen-url client (merge { :action            :post-points
                                        :precision         (write-options :precision)
                                        :consistency       (write-options :consistency)
                                        :retention-policy  (write-options :retention-policy) }
                                      default-write-options))
          body points]
      ((http-client/post url (merge {
                                    :body                  body
                                    :socket-timeout        5000           ;; in milliseconds
                                    :conn-timeout          5000           ;; in milliseconds
                                    :content-type          :text
                                    :throw-entire-message? true } (:post-opts client))) :status ))))

(defn escape-key
  [key]
  (-> key (.replace "," "\\,") (.replace " " "\\ ")))

(defn convert-tags-pairs
  "Converts seq of key-value pairs to influx 0.9 key-value pairs string"
  [pairs]
  (let [escaped (map (fn [[key val]][(escape-key key) (escape-key val)]) (partition 2 pairs))
        equaled (map  #(clojure.string/join "=" %) escaped)]
    (clojure.string/join "," equaled)))

(defn escape-field-key-value
  [[key val]]
  [(escape-key key) (str "\"" (.replace val "\"" "\\\"") "\"")])

(defn convert-fields-pairs
  [pairs]
  (let [escaped (map escape-field-key-value (partition 2 pairs))
        equaled (map  #(clojure.string/join "=" %) escaped)]
    (clojure.string/join "," equaled)))

(defn post-point-9
  "Post single point to influxDb-0.9. Tags and fields should be a seq of key-value pairs."
  [client key tags fields]
  (let [key-influx (escape-key key)
        tags-influx (convert-tags-pairs tags)
        fields-influx (convert-fields-pairs fields)]
    (post-points-9 client (str key-influx "," tags-influx " " fields-influx))))

(defn post-points
  "Post points to database based upon the InfluxDB version"
  ([client points]
   (case (:version client)
     "0.8" (post-points-8 client "testseries" points)
     "0.9" (post-points-9 client points)))
  ([client series-name points]
   (case (:version client)
     "0.8" (post-points-8 client series-name points)
     "0.9" (post-points-9 client points))))

;;
;; ## Query time-series
;;

(defn get-query-req
  "Submit query. Returns raw HTTP response."
  ([client query]
   (get-query-req client nil query))
  ([client time-precision query]
    (let [url (str (gen-url client {:action         :get-query
                                    :time-precision time-precision})
                                   (URLEncoder/encode query))]
      (http-client/get url (merge {
        :socket-timeout        10000  ;; in milliseconds
        :conn-timeout          10000  ;; in milliseconds
        :accept                :json
        :throw-entire-message? true }
        (:get-opts client))))))

(defn format-series-results
  {:no-doc true}
  [series-results]
  (let [points      (or (series-results "points") (series-results "values"))
        series-name (series-results "name")
        columns     (series-results "columns")
        column-keys (map keyword columns)
        column-maps (map #(zipmap column-keys %) points)]
    (map #(merge % {:name series-name}) column-maps)))

(defn format-results-8
  {:no-doc true}
  [results]
  (vec (flatten (map format-series-results results))))

(defn format-results-9
  {:no-doc true}
  [results]
  (vec (flatten (map format-series-results (flatten (map #(get % "series") (get results "results")))))))

(defn format-results
  {:no-doc true}
  [client results]
  (case (:version client)
    "0.8" (format-results-8 results)
    "0.9" (format-results-9 results)))

(defn read-result
  [client r]
  (format-results client (json/parse-string (r :body))))

(defn get-query
  "Submit query. Returns denormalized results set from string query."
  ([client query]
    (read-result client (get-query-req client query)))
  ([client time-precision query]
    (read-result client (get-query-req client time-precision query))))

(defn list-series
  "List all series in the current client database or in the specified database."
  ([{:keys [db] :as client}]
    (list-series client db))
  ([client database]
    (->> "list series"
         (get-query-req (merge client {:db database}))
         :body
         (#(json/parse-string % true))
         first
         :points
         (map second))))

;;
;; ## Returns the list of series for InfluxDB >=0.9
;; ## InfluxDB <= 0.8 has list-series
;;

(defn get-series-req
  "List databases. Returns raw HTTP response."
  [client]
  (let [url (gen-url client :get-series)
        uri (URLEncoder/encode "SHOW SERIES")]
    (http-client/get (str url uri) {
      :socket-timeout        1000  ;; in milliseconds
      :conn-timeout          1000  ;; in milliseconds
      :accept                :json
      :throw-entire-message? true })))

(defn get-series
  "Returns vector of database names."
  [client]
  (mapcat
   (fn [{:strs [columns values]}]
     (map (partial zipmap (map keyword columns)) values))
   (flatten (map #(get % "series") (get (json/parse-string ((get-series-req client) :body)) "results")))))
