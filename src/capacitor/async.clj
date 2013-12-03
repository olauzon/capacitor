(ns capacitor.async
  (:require [org.httpkit.client :as http-client]
            [cheshire.core      :as json]
            [clojure.core.async :as async])
  (:use     [capacitor.core :exclude [create-db-req
                                      create-db
                                      post-points-req
                                      post-points]]))

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
  (let [resp (create-db-req client)]
    @resp))

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
  "Post points to database. Returns HTTP status on success.
  Points should be submitted as a vector of maps."
  [client values]
  ;;(println "posting")
  (post-points-req client (make-payload values)))

(defn http-post-chan
  [url options]
  (let [c (async/chan)]
    ;;(println url)
    (http-client/post url options
      (fn [r] (async/put! c r)))
    c))

(defn http-get
  [url]
  (let [c (async/chan)]
    ;;(println url)
    (http-client/get url
      (fn [r] (async/put! c r)))
    c))

; Sliding buffer (aka. drop oldest)
(defn make-chan
  ([]
    (async/chan (async/sliding-buffer 100000)))
  ([s]
    (async/chan (async/sliding-buffer s))))

(defn enqueue
  [e-in e]
  (let [ts (System/currentTimeMillis)]
    (async/go (async/>! e-in
      (merge { :time ts } e)))))

(defn run!
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
          to ([t]
            (if (> (count points) 0)
              (do
                (async/go async/>! r-out
                  (post-points client points))
                (recur [] (async/timeout ms)))
              (recur points (async/timeout ms)))))))))
