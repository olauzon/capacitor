(ns capacitor.async
  (:require [org.httpkit.client :as http-client]
            [cheshire.core      :as json]
            [clojure.core.async :as async])
  (:use     [capacitor.core :exclude [create-db-req
                                      create-db
                                      post-points-req
                                      post-points
                                      get-query-req
                                      get-query]])
  (import [java.net URLEncoder]))

(defn make-chan
  "Make a sliding buffer channel for async input or output."
  ([]
    (async/chan (async/sliding-buffer 100000)))
  ([s]
    (async/chan (async/sliding-buffer s))))

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
  (async/go (async/>! r-in (create-db-req client))))

;;
;; ## Event buffering and queuing
;;

(defn enqueue
  "Append event to `e-in` channel.
  The `:time` attribute (in ms) is automatically added when the event is added."
  [e-in event]
  (let [ts (System/currentTimeMillis)]
    (async/put! e-in
      (merge { :time ts } event))))

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
  (let [cnt-size (dec size)]
    (async/thread
      (loop [points []
             to     (async/timeout msecs)]
        (async/alt!!
          e-in ([e]
            (if (>= (count points) cnt-size)
              (do
                (async/go async/>! r-out
                   (post-points client (conj points e)))
                  (recur [] (async/timeout msecs)))
              (recur (conj points e) to)))
          to ([_]
            (if (> (count points) 0)
              (do
                (async/go async/>! r-out
                  (post-points client points))
                (recur [] (async/timeout msecs)))
              (recur points (async/timeout msecs)))))))))

;;
;; ## Query time-series
;;

(defn get-query-req
  "Submit query. Returns raw HTTP response."
  [client query]
  (let [url   (str (gen-url client :get-query) (URLEncoder/encode query))
        c-out (async/chan)]
    (http-client/get url {
      :socket-timeout        10000  ;; in milliseconds
      :conn-timeout          10000  ;; in milliseconds
      :accept                :json
      :throw-entire-message? true }
      (fn [r] 
        (async/put! c-out r)))
    c-out))

(defn read-results
  [r-out]
  (async/go
    (loop []
      (when-let [r (async/<! r-out)]
        (println (read-result (async/<! r)))
        (recur)))))

(defn get-query
  "Submit query. Returns results in `r-out` channel."
  [client query r-out]
  (let [r (get-query-req client query)]
    (async/put! r-out r)))
