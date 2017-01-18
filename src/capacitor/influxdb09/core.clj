(ns capacitor.influxdb09.core
  (:require [clj-http.client :as http-client]
            [clojure.set :as set]
            [cheshire.core   :as json])
  (import [java.net URLEncoder]))

;;
;; ## Client options
;;

(def default-client
  "Default HTTP client configuration"
   { :host              "localhost"
     :scheme            "http"
     :port              8086
     :db                "testdb"
     :username          "root"
     :password          "root" })

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
      :host             (default: \"localhost\")
      :scheme           (default: \"http://\")
      :port             (default: 8086)
      :db               (default: \"default-db\")
      :username         (default \"root\")
      :password         (default \"root\")
      :post-opts        (http post options, default: \"nil\")
      :get-opts         (http get options, default: \"nil\")"
  [opts]
  (merge default-client opts))

(defn gen-url-fn
  "Returns a generated URL according to the operation type."
  [client action]
  (str
    (client :scheme)
    "://"
    (client :host)
    ":"
    (client :port)
    (cond
      (= (action :action) :post-points) (str "/write?"
                                             "db=" (client :db) "&"
                                             "rp=" (action :retention-policy) "&"
                                             "precision=" (action :precision) "&"
                                             "consistency=" (action :consistency) "&"
                                             "u=" (client :username) "&"
                                             "p=" (client :password)))))

(defmulti gen-url-multi
  (fn [_ action] (class action)))

(defmethod gen-url-multi clojure.lang.Keyword
  [client action]
  (gen-url-fn client { :action action }))

(defmethod gen-url-multi clojure.lang.PersistentHashMap
  [client action]
  (gen-url-fn client action))

(defmethod gen-url-multi clojure.lang.PersistentArrayMap
  [client action]
  (gen-url-fn client action))

(def gen-url
  (memoize gen-url-multi))

;;
;; ## Post time-series points
;;

(defn post-points
  "Post points to database. Returns HTTP status on success.
   Uses influxdb's http line protocol to submit data-points"
  ([client points]
    (post-points client points { :precision        nil
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


