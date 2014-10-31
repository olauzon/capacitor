(ns capacitor.async
  (:require
    [capacitor.accumulator :as acc]
    [org.httpkit.client :as http-client]
    [cheshire.core      :as json]
    [clojure.tools.logging :as log]
    [clojure.core.async :refer [chan
                                sliding-buffer
                                go
                                >!
                                >!!
                                put!
                                thread
                                timeout
                                alt!!
                                <!
                                <!!]])
  (:use [capacitor.core :exclude [create-db-req
                                  create-db
                                  post-points-req
                                  post-points
                                  get-query-req
                                  get-query]])
  (import [java.net URLEncoder]))

(defn make-chan
  "Make a sliding buffer channel for async input or output."
  ([]
    (chan (sliding-buffer 100000)))
  ([s]
    (chan (sliding-buffer s))))

;;
;; ## Basic async API
;;

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
  "Create database defined in client. Returns HTTP response in `r-in` channel."
  [client r-in]
  (go (>! r-in (create-db-req client))))

;;
;; ## Event buffering and queuing
;;

(defn enqueue
  "Append event to `e-in` channel.
  The `:time` attribute (in ms) is automatically added when the event is added and no existing attribute is found."
  [e-in event]
  (let [ts (or (:time event) (System/currentTimeMillis))]
    (put! e-in (merge { :time ts } event))))

(defn post-points-req
  [client points]
  (let [url  (gen-url client :post-points)
        body (json/generate-string points)
        opts (merge {:body                  body
                     ;;:timeout             60000 ;; in milliseconds
                     :content-type          :json
                     :throw-entire-message? true}
                    (:async-post-opts client))]
    (http-client/post url opts
                      (fn [{:keys [status error]}] ;; asynchronous response handling
                        (if error
                          (log/warnf error "Failed to post %d point(s), received status %s" (count points) status)
                          (log/debugf "async http post: %s" status))))))

(defn post-points
  "Post points to database. Returns HTTP response.
  Points should be submitted as a vector of maps."
  [client values]
  (if (log/enabled? :debug)
    (log/debugf "Posting %d value(s): %s" (count values) values))
  (post-points-req client (make-payload values)))

;;
;; ### Main run loop
;;

(defn post-loop!
  [r-out client]
  (thread
    (loop []
    (when-let [points (<!! r-out)]
      (do 
        (post-points client points)
        (recur))))))

(defn run!
  [e-in r-out client size msecs]
  (acc/run! e-in r-out size msecs)
  (post-loop! r-out client))

;;
;; ## Query time-series
;;

(defn get-query-req
  "Submit query. Returns raw HTTP response."
  [client query]
  (let [url   (str (gen-url client :get-query) (URLEncoder/encode query))
        c-out (chan)]
    (http-client/get url (merge {
      :socket-timeout        60000  ;; in milliseconds
      :conn-timeout          60000  ;; in milliseconds
      :accept                :json
      :throw-entire-message? true }
      (:async-get-opts client))
      (fn [r]
        (put! c-out r)))
    c-out))

(defn read-results
  [r-out]
  (go
    (loop []
      (when-let [r (<! r-out)]
        (println (read-result (<! r)))
        (recur)))))

(defn get-query
  "Submit query. Returns results in `r-out` channel."
  [client query r-out]
  (let [r (get-query-req client query)]
    (put! r-out r)))
