(ns capacitor.http
  (:require [clojure.string :refer [split escape join upper-case]]
            [clj-http.client :as http-client]
            [clojure.algo.generic.functor :refer [fmap]])
  (:import [java.net URLEncoder]))


;; Results
;;
(defn ->maps
  [s]
  (assoc s :results (vec (map #(zipmap (:columns s) %) (:values s)))))

(defn fmap-func
  [depth func data]
  (if (zero? depth) (func data) (fmap #(fmap-func (dec depth) func %) data)))

(defn results->maps
  [results]
  (fmap-func 4 ->maps results))

(defn extract
  [results & [results->maps?]]
  (if results->maps? (-> results :body results->maps) (-> results :body)))

;;
;; ## Client options
;;
(def default-client
  "Default HTTP client configuration"
   {:host     "localhost"
    :scheme   "http"
    :port     8086
    :username "root"
    :password "root"
    :db       "testdb"
    :version  "0.9"})

(defn make-client
  [opts]
  (merge default-client opts))

(def default-get-opts
  {:socket-timeout        1000 ;; in ms
   :conn-timeout          1000 ;; in ms
   :accept                :json
   :throw-entire-message? true})

(defn gen-url-fn
  ^{:no-doc true
    :private true}
  [client action]
  (str
    (client :scheme)
    "://"
    (client :host)
    ":"
    (client :port)
    (str
      (cond
        (= :ping action)
          "/ping"
        (= :write action)
          (str "/write?"
               "db=" (client :db)
               "&"
               "u=" (client :username)
               "&"
               "p=" (client :password))
        (contains? #{:db-query :list-series} action)
          (str "/query?"
               "db=" (client :db)
               "&"
               "u=" (client :username)
               "&"
               "p=" (client :password)
               "&"
               "q=")
        :else
          (str "/query?"
               "u=" (client :username)
               "&"
               "p=" (client :password)
               "&"
               "q=")))))

(def gen-url
  (memoize gen-url-fn))

(defn execute
  [client action query]
  (let [url  (gen-url client action)
        q    (URLEncoder/encode query)
        opts (merge default-get-opts (:get-opts client) {:as :json})]
    (http-client/get (str url q) opts)))

(defn post
  [client action data]
  (let [url (gen-url client action)]
    (http-client/post url {:body data})))
