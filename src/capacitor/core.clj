(ns capacitor.core
  (:require [clj-http.client :as http-client]
            [cheshire.core   :as json])
  (import [java.net URLEncoder]))

(def default-client
  "Default HTTP client configuration"
   {  :host     "localhost"
      :scheme   "http"
      :port     8086
      :username "root"
      :password "root"
      :db       "default-db" })

(defn make-client
  "Returns a map representing an HTTP client configuration.

    Valid options:
      :hostname  the hostname to connect to (default: \"localhost\")
      :scheme \"http://\" or \"https://\" (default: \"http://\")
      :port the port to connect to (default: 8086)
      :username the username to use when executing commands (default \"root\")
      :password the password associated with the username (default \"root\")
      :db the database to use (default: \"default-db\")"
  [opts]
  (merge default-client opts))

(defn gen-url-fn
  {:no-doc true}
  [client action]
  (str
    (client :scheme)
    "://"
    (client :host)
    ":"
    (client :port)
    "/db"
    (cond
      (= action :delete-db)      (str "/" (client :db))
      (= action :get-dbs)        "s"
      (= action :create-db-user) (str "/" (client :db) "/users")
      (or (= action :post-points) (= action :get-query))
          (str "/" (client :db) "/series"))
    "?u="
    (client :username)
    "&p="
    (client :password)
    (cond
      (= action :get-query) "&q=")))

(def gen-url
  (memoize gen-url-fn))

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

(defn delete-db-req
  "Delete database defined in client. Returns raw HTTP response."
  [client]
  (let [url (gen-url client :delete-db)]
    (http-client/delete url {
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :throw-entire-message? true })))

(defn delete-db
  "Delete database defined in client. Returns HTTP status on success."
  [client]
  ((delete-db-req client) :status))

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

(defn post-points-req
  [client points]
  (let [url  (gen-url client :post-points)
        body (json/generate-string points)]
    (http-client/post url {
      :body                  body
      :socket-timeout        1000 ;; in milliseconds
      :conn-timeout          1000 ;; in milliseconds
      :content-type          :json
      :throw-entire-message? true })))

(defn format-payload
  [res]
  (let [res-seq (seq res)
        seq-cnt (dec (count res-seq))]
  (loop [curr-res []
         i 0]
    (let [rec (nth res-seq i)
          series-name (first rec)
          values (last rec)
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
  [client series-name values]
  ((post-points-req client (make-payload series-name values)) :status))

(defn get-query-req
  "Submit query. Returns raw HTTP response."
  [client query]
  (let [url (str (gen-url client :get-query) (URLEncoder/encode query))]
    (http-client/get url {
      ;;:socket-timeout        1000  ;; in milliseconds
      ;;:conn-timeout          1000  ;; in milliseconds
      :accept                :json
      :throw-entire-message? true })))

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

(defn get-query
  "Submit query. Returns denormalized results set from string query"
  [client query]
  (format-results (json/parse-string ((get-query-req client query) :body))))
