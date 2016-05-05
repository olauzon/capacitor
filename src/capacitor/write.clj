(ns capacitor.write
  (:require [capacitor.http :as http :only [post]]))

(defn post-req
  [client data]
  (http/post client :write data))

(defn post
  [client data]
  (= 204 (:status (post-req client data))))
