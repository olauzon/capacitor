(ns capacitor.http-test
  (:require [clojure.test   :refer :all]
            [capacitor.http :refer :all]
            [clojure.pprint :refer [pprint]]))
;;
;; (def fixture-results-00
;;   '(
;;     { "name"    "logins"
;;       "columns" ["time" "sequence_number" "count"]
;;       "points"  [
;;         [1384657020000 1 14]
;;         [1384655880000 1 14]
;;         [1384655820000 1 28]
;;         [1384655520000 1 7] ]}))
;;
;;
(def fixture-query-response-00
  {:orig-content-encoding "gzip",
   :trace-redirects
   ["http://localhost:8086/query?db=mynewdb&u=myuser&p=mypassword&q=SELECT+*+FROM+memory%3BSELECT+*+FROM+cpu_load%3BSELECT+*+FROM+cpu_count"],
   :request-time 25,
   :status 200,
   :headers
   {"Connection" "close",
    "Content-Length" "174",
    "Date" "Fri, 01 Apr 2016 21:42:17 GMT",
    "X-Influxdb-Version" "0.11.0",
    "Request-Id" "9b2382cc-f852-11e5-8026-000000000000",
    "Content-Type" "application/json"},
   :body
   {:results
    [{:series
      [{:name "memory",
        :columns ["time" "dc" "host" "value"],
        :values
        [["2016-03-10T15:40:12Z" "1" "1" 0.8]
         ["2016-03-10T15:40:13Z" "1" "1" 0.6]]}]}
     {:series
      [{:name "cpu_load",
        :columns ["time" "dc" "host" "value"],
        :values
        [["2016-03-10T15:40:12Z" "1" "1" 1.1]
         ["2016-03-10T15:40:13Z" "1" "1" 0.9]]}]}
     {:series
      [{:name "cpu_count",
        :columns ["time" "dc" "host" "value"],
        :values
        [["2016-03-10T15:40:12Z" "1" "1" 4]
         ["2016-03-10T15:40:13Z" "1" "1" 4]]}]}]}})

(deftest test-client-00
  (testing "Default"
    (is (=
      {
        :host     "localhost"
        :scheme   "http"
        :port     8086
        :username "root"
        :password "root"
        :db       "testdb"
        :version  "0.9"
      }
      (make-client {})))))

(deftest test-client-01
  (testing "Custom"
    (is (=
      {
        :host     "influx.myapp.com"
        :scheme   "https"
        :port     443
        :username "hello"
        :password "world"
        :db       "my-database-name"
        :version  "0.9"
      }
      (make-client {
        :host     "influx.myapp.com"
        :scheme   "https"
        :port     443
        :username "hello"
        :password "world"
        :db       "my-database-name" })))))

(deftest test-gen-url-00
  (testing "create-db"
    (is (= "http://localhost:8086/query?u=root&p=root&q="
           (gen-url (make-client {}) :create-db)))))

(deftest test-extract-results-00
  (is (=
        {:results
          [{:series
            [{:name    "memory",
              :columns ["time" "dc" "host" "value"],
              :values  [["2016-03-10T15:40:12Z" "1" "1" 0.8]
                        ["2016-03-10T15:40:13Z" "1" "1" 0.6]]}]}
           {:series
            [{:columns ["time" "dc" "host" "value"],
              :name "cpu_load",
              :values
              [["2016-03-10T15:40:12Z" "1" "1" 1.1]
               ["2016-03-10T15:40:13Z" "1" "1" 0.9]]}]}
           {:series
            [{:columns ["time" "dc" "host" "value"],
              :name "cpu_count",
              :values
              [["2016-03-10T15:40:12Z" "1" "1" 4]
               ["2016-03-10T15:40:13Z" "1" "1" 4]]}]}]}
         (extract fixture-query-response-00))))


(deftest test-extract-results->maps-00
  (is (= {:results
          [{:series
            [{:columns ["time" "dc" "host" "value"],
              :name "memory",
              :values
              [["2016-03-10T15:40:12Z" "1" "1" 0.8]
               ["2016-03-10T15:40:13Z" "1" "1" 0.6]],
              :results
              [{"dc" "1",
                "host" "1",
                "value" 0.8,
                "time" "2016-03-10T15:40:12Z"}
               {"dc" "1",
                "host" "1",
                "value" 0.6,
                "time" "2016-03-10T15:40:13Z"}]}]}
           {:series
            [{:columns ["time" "dc" "host" "value"],
              :name "cpu_load",
              :values
              [["2016-03-10T15:40:12Z" "1" "1" 1.1]
               ["2016-03-10T15:40:13Z" "1" "1" 0.9]],
              :results
              [{"dc" "1",
                "host" "1",
                "value" 1.1,
                "time" "2016-03-10T15:40:12Z"}
               {"dc" "1",
                "host" "1",
                "value" 0.9,
                "time" "2016-03-10T15:40:13Z"}]}]}
           {:series
            [{:columns ["time" "dc" "host" "value"],
              :name "cpu_count",
              :values
              [["2016-03-10T15:40:12Z" "1" "1" 4]
               ["2016-03-10T15:40:13Z" "1" "1" 4]],
              :results
              [{"dc" "1", "host" "1", "value" 4, "time" "2016-03-10T15:40:12Z"}
               {"dc" "1",
                "host" "1",
                "value" 4,
                "time" "2016-03-10T15:40:13Z"}]}]}]}
         (extract fixture-query-response-00 true))))
