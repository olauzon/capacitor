(ns capacitor.core
  (:require [clj-http.client :as http-client]
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
     :db          "default-db"})

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
;; ## HTTP URL generation
;;

(defn gen-url-fn
  {:no-doc true}
  [client action]
  (str
    (client :scheme)
    "://"
    (client :host)
    ":"
    (client :port)
    (cond (contains? #{:ping} (action :action))
          "/ping"
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
                        :drop-shard-space
                        :create-shard-space } (action :action))
          (str
           "/cluster"
           (cond
             (= (action :action) :get-shard-spaces) "/shard_spaces"
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

(defn ping
  [client]
  (-> client
      (gen-url :ping)
      (http-client/get 
        (merge {:socket-timeout       1000
                :conn-timeout         1000
                :accept               :json
                :trow-entire-message? true}
               (:get-opts client)))
      :body
      kw-parse-string))

;;
;; ## Database management
;;

;;;;;;;;;;;;;;;;;;;;;;;;
;; ### Create a database

(defn create-db-req
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

(defn create-db
  "Create database defined in client. Returns HTTP status on success."
  [client]
  ((create-db-req client) :status))

;;;;;;;;;;;;;;;;;;;;;
;; ### List databases

(defn get-dbs-req
  "List databases. Returns raw HTTP response."
  [client]
  (let [url (gen-url client :get-dbs)]
    (http-client/get url {
      :socket-timeout        1000  ;; in milliseconds
      :conn-timeout          1000  ;; in milliseconds
      :accept                :json
      :throw-entire-message? true })))

(defn get-dbs
  "Returns vector of database names."
  [client]
  (json/parse-string ((get-dbs-req client) :body)))

;;;;;;;;;;;;;;;;;;;;;;
;; ### Drop a database

(defn delete-db-req
  "Delete database defined in client. Returns raw HTTP response."
  [client]
  (let [url (gen-url client :delete-db)]
    (http-client/delete url {
      :socket-timeout        10000 ;; in milliseconds
      :conn-timeout          10000 ;; in milliseconds
      :throw-entire-message? true })))

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
  (json/parse-string ((get-admin-users-req client) :body)))

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
  (json/parse-string ((get-db-users-req client) :body)))

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
  (json/parse-string ((get-shards-req client) :body)))

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
  (json/parse-string ((get-shard-spaces-req client) :body)))


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
;; ## Post time-series points
;;

(defn post-points-req
  ([client points]
    (post-points-req client points nil))
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

(defn post-points
  "Post points to database. Returns HTTP status on success.
  Points should be submitted as a vector of maps."
  ([client series-name values]
    ((post-points-req client (make-payload series-name values)) :status))
  ([client series-name time-precision values]
    ((post-points-req client
                      (make-payload series-name values)
                      time-precision) :status)))

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
  (let [points      (series-results "points")
        series-name (series-results "name")
        columns     (series-results "columns")
        column-keys (map keyword columns)
        column-maps (map #(zipmap column-keys %) points)]
    (map #(merge % {:name series-name}) column-maps)))

(defn format-results
  {:no-doc true}
  [results]
  (vec (flatten (map format-series-results results))))

(defn read-result
  [r]
  (format-results (json/parse-string (r :body))))

(defn get-query
  "Submit query. Returns denormalized results set from string query."
  ([client query]
    (read-result (get-query-req client query)))
  ([client time-precision query]
    (read-result (get-query-req client time-precision query))))
