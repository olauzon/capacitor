(ns capacitor.core-test
  (:require [clojure.test   :refer :all]
            [capacitor.core :refer :all]))

(def fixture-results-00
  '(
    { "name"    "logins"
      "columns" ["time" "sequence_number" "count"]
      "points"  [
        [1384657020000 1 14]
        [1384655880000 1 14]
        [1384655820000 1 28]
        [1384655520000 1 7] ]}))

(deftest test-client-00
  (testing "Default"
    (is (=
      {
        :host     "localhost"
        :scheme   "http"
        :port     8086
        :username "root"
        :password "root"
        :db       "default-db"
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
    (is (= "http://localhost:8086/db?u=root&p=root"
           (gen-url (make-client {}) :create-db)))))

(deftest test-gen-url-01
  (testing "delete-db"
    (is (= "http://localhost:8086/db/my-database-name?u=root&p=root"
           (gen-url (make-client {:db "my-database-name"}) :delete-db)))))

(deftest test-gen-url-02
  (testing "get-dbs"
    (is (= "http://localhost:8086/dbs?u=root&p=root"
           (gen-url (make-client {}) :get-dbs)))))

(deftest test-gen-url-03
  (testing "create-db-user"
    (is (= "http://localhost:8086/db/my-db/users?u=root&p=root"
           (gen-url (make-client {:db "my-db"}) :create-db-user)))))

(deftest test-gen-url-04
  (testing "post-points"
    (is (= "http://localhost:8086/db/my-db/series?u=root&p=root"
           (gen-url (make-client {:db "my-db"}) :post-points)))))

(deftest gen-url-05
  (testing "get-query"
    (is (= "http://localhost:8086/db/my-db/series?u=root&p=root&q="
           (gen-url (make-client {:db "my-db"}) :get-query)))))

(deftest gen-url-05
  (testing "get-query"
    (is (= "http://localhost:8086/db/my-db/series?u=root&p=root&q="
           (gen-url (make-client {:db "my-db"}) :get-query)))))

(deftest test-format-results-00
  (testing "format-results"
    (is (= [
             {
                :name "logins"
                :time 1384657020000
                :sequence_number 1
                :count 14
              }
             {
                :name "logins"
                :time 1384655880000
                :sequence_number 1
                :count 14
              }
             {
                :name "logins"
                :time 1384655820000
                :sequence_number 1
                :count 28
              }
             {
                :name "logins"
                :time 1384655520000
                :sequence_number 1
                :count 7
              }
            ]
           (format-results fixture-results-00)))))

(deftest test-make-payload-00
  (testing "format-results" (is (=
      [
        {
          :name    "test-series"
          :points  '(("miles@gmail.com")
                     ("charlie@gmail.com")
                     ("john@gmail.com"))
          :columns '(:email)
         }
       ]
    (make-payload "test-series" [
        {:email "miles@gmail.com"}
        {:email "charlie@gmail.com"}
        {:email "john@gmail.com"}
      ])))))

(deftest test-make-payload-00
  (testing "format-results" (is (=
      [
        {
          :name    "test-series-00"
          :points  '(("miles@gmail.com" "111")
                     ("charlie@gmail.com" nil))
          :columns '(:email :addr)
         }
        {
          :name    "test-series-01"
          :points  '(("john@gmail.com"))
          :columns '(:email)
         }
       ]
    (make-payload [
        {:email "miles@gmail.com"
         :addr  "111"
         :series "test-series-00"}
        {:email "charlie@gmail.com"
         :series "test-series-00"}
        {:email "john@gmail.com"
         :series "test-series-01"}
      ])))))
