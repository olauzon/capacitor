(ns capacitor.point
  (:require [clojure.string :refer [escape join]]))

(defn escape-str
  [k]
  (escape (str k) {\space "\\ " \, "\\,"}))

(defn convert-value
  [v]
  (cond (string? v) (str "\"" (.replace ^String v "\"" "\\\"") "\"")
        (integer? v) (str v "i")
        :else v))

(defn pad-fn
  [length]
  (vec (repeat (- 19 length) "0")))

(def pad (memoize pad-fn))

(defprotocol ToLine
  (->line [_]))

(defprotocol ToString
  (->str [_]))

(defrecord Tag
  [k v]
  ToString
  (->str [f] (str (escape-str k) "=" (escape-str v))))

(defrecord Field
  [k v]
  ToString
  (->str [f] (str (escape-str k) "=" (convert-value v))))

(defrecord Timestamp
  [ts]
  ToString
  (->str [ts]
    (let [v (->> ts :ts (map str) vec)] (join (into v (pad (count v)))))))

(defrecord Point
  [measurement tags fields timestamp]
  ToLine
  (->line [_]
    (str
      (escape-str measurement)
      (when-not (empty? tags)
        (str ","
             (apply str (interpose "," (map ->str (sort-by :k tags))))))
      (when-not (empty? fields)
        (str " " (apply str (interpose "," (map ->str fields)))))
      (when-not (nil? timestamp)
        (str " " (->str timestamp)))
      "\n")))

(derive clojure.lang.PersistentArrayMap ::map)
(derive clojure.lang.PersistentHashMap  ::map)

(derive clojure.lang.PersistentList   ::list)
(derive clojure.lang.PersistentVector ::list)
(derive clojure.lang.MapEntry         ::list)

(defmulti  ->tag class)
(defmethod ->tag Tag    [t]     t)
(defmethod ->tag ::list [[k v]] (->Tag k v))
(defmethod ->tag ::map  [m]     (map ->tag m))

(defmulti  ->field class)
(defmethod ->field Field  [f]     f)
(defmethod ->field ::list [[k v]] (->Field k v))
(defmethod ->field ::map  [m]     (map ->field m))

(defmulti  ->timestamp class)
(defmethod ->timestamp nil              [ts] ts)
(defmethod ->timestamp Timestamp        [ts] ts)
(defmethod ->timestamp java.lang.String [ts] (->Timestamp ts))
(defmethod ->timestamp java.lang.Long   [ts] (->Timestamp (str ts)))

(defn point
  [measurement tags fields timestamp]
  (->Point measurement
           (map ->tag tags)
           (map ->field fields)
           (->timestamp timestamp)))

(defmulti  ->point class)
(defmethod ->point Point [t] t)
(defmethod ->point ::map [m]
  (point (:measurement m) (:tags m) (:fields m) (:timestamp m)))
(defmethod ->point ::list [[measurement tags fields timestamp]]
  (point measurement tags fields timestamp))

(defmulti  ->lines class)
(defmethod ->lines Point  [p]  (->line p))
(defmethod ->lines ::list [ps] (join (map ->line ps)))
