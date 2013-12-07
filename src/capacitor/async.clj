(ns capacitor.async
  (:require [org.httpkit.client :as http-client]
            [cheshire.core      :as json]
            [clojure.core.async :as async])
  (:use     [capacitor.core :exclude [create-db-req
                                      create-db
                                      post-points-req
                                      post-points]]))

(defn make-chan
  "Make a sliding buffer channel for async input or output."
  ([]
    (async/chan (async/sliding-buffer 100000)))
  ([s]
    (async/chan (async/sliding-buffer s))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ## Basic async API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ## Event buffering and queuing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn enqueue
  "Append event to `e-in` channel.
  The `:time` attribute (in ms) is automatically added when the event is added."
  [e-in event]
  (let [ts (System/currentTimeMillis)]
    (async/go (async/>! e-in
      (merge { :time ts } event)))))

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
  "Buffers events accumulating from `e-in` for a maximum batch of `size` or
  `ms` milliseconds. Responses are returned in `r-out` to be consumed by a
  monitoring loop."
  [e-in r-out client size ms]
  (let [cnt-size (dec size)]
    (async/thread
      (loop [points []
             to     (async/timeout ms)]
        (async/alt!!
          e-in ([e]
            (if (>= (count points) cnt-size)
              (do
                (async/go async/>! r-out
                   (post-points client (conj points e)))
                  (recur [] (async/timeout ms)))
              (recur (conj points e) to)))
          to ([_]
            (if (> (count points) 0)
              (do
                (async/go async/>! r-out
                  (post-points client points))
                (recur [] (async/timeout ms)))
              (recur points (async/timeout ms)))))))))
