(ns capacitor.influxdb09.core-test
  (:require [clojure.test   :refer :all]
            [capacitor.influxdb09.core :refer :all]))


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
        :db       "mydb"
      }
      (make-client {
        :host     "influx.myapp.com"
        :scheme   "https"
        :port     443
        :username "hello"
        :password "world"
        :db       "mydb" })))))

(deftest test-gen-url-00
  (testing "post-points"
    (is (= "http://localhost:8086/write?db=testdb&rp=&precision=&consistency=&u=root&p=root"
           (gen-url (make-client {}) :post-points)))))
