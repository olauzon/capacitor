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
    (is (= "http://localhost:8086/db?u=root&p=root"
           (gen-url (make-client {}) :get-dbs)))))

(deftest test-gen-url-03
  (testing "create-db-user"
    (is (= "http://localhost:8086/db/my-db/users?u=root&p=root"
           (gen-url (make-client {:db "my-db"}) :create-db-user)))))

(deftest test-gen-url-04
  (testing "create-admin-user"
    (is (= "http://localhost:8086/cluster_admins?u=root&p=root"
           (gen-url (make-client {:db "my-db"}) :create-admin-user)))))

(deftest test-gen-url-05
  (testing "post-points"
    (is (= "http://localhost:8086/db/my-db/series?u=root&p=root"
           (gen-url (make-client {:db "my-db"}) :post-points)))))

(deftest test-gen-url-06
  (testing "post-points with time_precision"
    (is
      (= "http://localhost:8086/db/my-db/series?u=root&p=root&time_precision=u"
         (gen-url (make-client {:db "my-db"}) { :action         :post-points
                                                :time-precision "u" })))))

(deftest test-gen-url-07
  (testing "get-query"
    (is (= "http://localhost:8086/db/my-db/series?u=root&p=root&q="
           (gen-url (make-client {:db "my-db"}) :get-query)))))

(deftest test-gen-url-08
  (testing "get-query with time_precision"
    (is (=
      "http://localhost:8086/db/my-db/series?u=root&p=root&time_precision=u&q="
      (gen-url (make-client {:db "my-db"}) { :action         :get-query
                                             :time-precision "u" })))))

(deftest test-gen-url-09
  (testing "delete-db-user"
    (is (= "http://localhost:8086/db/my-db/users/my-username?u=root&p=root"
           (gen-url (make-client {:db "my-db"}) { :action   :delete-db-user
                                                  :username "my-username" })))))

(deftest test-gen-url-10
  (testing "delete-admin-user"
    (is (= "http://localhost:8086/cluster_admins/my-username?u=root&p=root"
           (gen-url (make-client {:db "my-db"}) { :action   :delete-admin-user
                                                  :username "my-username" })))))

(deftest test-gen-url-11
  (testing "update-db-user"
    (is (= "http://localhost:8086/db/my-db/users/my-username?u=root&p=root"
           (gen-url (make-client {:db "my-db"}) { :action   :update-db-user
                                                  :username "my-username" })))))

(deftest test-gen-url-12
  (testing "update-admin-user"
    (is (= "http://localhost:8086/cluster_admins/my-username?u=root&p=root"
           (gen-url (make-client {:db "my-db"}) { :action   :update-admin-user
                                                  :username "my-username" })))))

(deftest test-gen-url-13
  (testing "delete-series"
    (is (= "http://localhost:8086/db/my-db/series/my-series?u=root&p=root"
           (gen-url (make-client {:db "my-db"}) { :action   :delete-series
                                                  :series   "my-series" })))))

(deftest test-gen-url-14
  (testing "get-shards"
    (is (= "http://localhost:8086/cluster/shards?u=root&p=root"
           (gen-url (make-client {:db "my-db"}) :get-shards)))))

(deftest test-gen-url-15
  (testing "get-shard-spaces"
    (is (= "http://localhost:8086/cluster/shard_spaces?u=root&p=root"
           (gen-url (make-client {:db "my-db"}) :get-shard-spaces)))))

(deftest test-client-16
  (testing "drop-shard-space"
    (is (= "http://localhost:8086/cluster/shard_spaces/my-db/default?u=root&p=root"
           (gen-url (make-client {:db "my-db"
                                  :shard-space "default"}) :drop-shard-space)))))

(deftest test-client-17
  (testing "drop-shard"
    (is (= "http://localhost:8086/cluster/shards/1?u=root&p=root"
           (gen-url (make-client {:db "my-db"
                                  :shard-id 1}) :drop-shard)))))

(deftest test-gen-url-18
  (testing "ping"
    (is (= "http://localhost:8086/ping"
           (gen-url (make-client {}) :ping)))))

(deftest test-gen-url-19
  (testing "sync"
    (is (= "http://localhost:8086/sync?u=root&p=root"
           (gen-url (make-client {}) :sync)))))

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
