(defproject capacitor "0.2.3"
  :description "A Clojure client for InfluxDB"
  :url         "https://github.com/olauzon/capacitor"
  :license {
    :name "Eclipse Public License"
    :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure    "1.5.1"]
    [org.clojure/core.async "0.1.303.0-886421-alpha"]
    [clj-http               "0.9.1"]
    [http-kit               "2.1.18"]
    [cheshire               "5.3.1"]
  ]
  :plugins [
    [codox           "0.6.6"]
    [lein-marginalia "0.7.1"]
  ]
  :dev-dependencies [
    [codox "0.6.6"]
  ]
  :codox {
    :output-dir "docs/codox"
    ;;:src-dir-uri "https://github.com/olauzon/capacitor/blob/v0.1.3/"
    :src-dir-uri "https://github.com/olauzon/capacitor/blob/master/"
    :src-linenum-anchor-prefix "L"
  }
  :aot :all)
