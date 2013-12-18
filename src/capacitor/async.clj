(ns capacitor.async
  (:require
    [org.httpkit.client :as http-client]
    [cheshire.core      :as json]
    [clojure.core.async :refer [chan
                                sliding-buffer
                                go
                                >!
                                put!
                                thread
                                timeout
                                alt!!
                                <!]])
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
  The `:time` attribute (in ms) is automatically added when the event is added."
  [e-in event]
  (let [ts (System/currentTimeMillis)]
    (put! e-in (merge { :time ts } event))))

(defn post-points-req
  [client points]
  (let [url  (gen-url client :post-points)
        body (json/generate-string points)]
    (http-client/post url {
      :body                  body
      ;;:socket-timeout        1000 ;; in milliseconds
      ;;:conn-timeout          1000 ;; in milliseconds
      :content-type          :json
      :throw-entire-message? true })))

(defn post-points
  "Post points to database. Returns HTTP response.
  Points should be submitted as a vector of maps."
  [client values]
  ;;(println "posting")
  (post-points-req client (make-payload values)))

;;
;; ### Main run loop
;;

(defn run!
  "Buffer events accumulating from `e-in` for a maximum batch of `size` or
  `msecs` milliseconds. Responses are returned in `r-out` to be consumed by a
  monitoring loop."
  [e-in r-out client size msecs]
  (thread
    (loop [points []
           to     (timeout msecs)]
      (alt!!
        e-in ([e]
          (if (nil? e)
            (if-not (empty? points)
              (go (>! r-out (post-points client points))))
            (if (> size (count points))
              (recur (conj points e) to)
              (do
                (go (>! r-out (post-points client (conj points e))))
                (recur [] (timeout msecs))))))
        to ([_]
          (if (empty? points)
            (recur points (timeout msecs))
            (do
              (go (>! r-out (post-points client points)))
              (recur [] (timeout msecs)))))))
    (println "run! loop stopped")))

;;
;; ## Query time-series
;;

(defn get-query-req
  "Submit query. Returns raw HTTP response."
  [client query]
  (let [url   (str (gen-url client :get-query) (URLEncoder/encode query))
        c-out (chan)]
    (http-client/get url {
      :socket-timeout        10000  ;; in milliseconds
      :conn-timeout          10000  ;; in milliseconds
      :accept                :json
      :throw-entire-message? true }
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
