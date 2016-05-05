(ns capacitor.point-test
  (:require [clojure.test :refer :all]
            [capacitor.point :refer :all]))


(def fixture-point-map
  (->point
    {:measurement "cpu_load"
     :tags        {"host" "1" "dc" 1}
     :fields      {"value" 1.1}
     :timestamp   1457624412}))

(def fixture-point-vector
  (->point
  ;; measurement tags                fields        timestamp
    ["cpu_load"  {"host" "1" "dc" 1} {"value" 1.2} 1457624413]))

(def fixture-point-map-of-vectors
  (->point
    {:measurement "cpu_load"
     :tags        [["host" "1"] ["dc" 3]]
     :fields      [["value" 1.0] ["value_int" 1] ["web" "app"]]
     :timestamp   1457624413}))

(def fixture-point-vector-of-vectors
  (->point
    ["cpu_load"                                       ;; measurement
     [(list "host" "1") ["dc" 3]]                     ;; tags
     [["value" 1.0] ["value_int" 1] ["web" "app"]]    ;; fields
     1457624413]))                                    ;; timestamp

(deftest test-escape-str-0
  (is (= "" (escape-str ""))))

(deftest test-escape-str-1
  (is (= "" (escape-str nil))))

(deftest test-escape-str-2
  (is (= "app\\,le\\,\\ \\ or\\,ange\\ s" (escape-str "app,le,  or,ange s"))))

(deftest test-point-map->line
  (is (= "cpu_load,dc=1,host=1 value=1.1 1457624412000000000\n"
         (->line fixture-point-map))))

(deftest test-point-vector->line
  (is (= "cpu_load,dc=1,host=1 value=1.2 1457624413000000000\n"
         (->lines fixture-point-vector))))

(deftest test-point-map-of-vectors->line
  (is (= "cpu_load,dc=3,host=1 value=1.0,value_int=1i,web=\"app\" 1457624413000000000\n"
         (->lines fixture-point-map-of-vectors))))

(deftest test-point-vector-of-vectors->line
  (is (= "cpu_load,dc=3,host=1 value=1.0,value_int=1i,web=\"app\" 1457624413000000000\n"
         (->lines fixture-point-vector-of-vectors))))
