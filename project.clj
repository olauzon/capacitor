(defproject capacitor "0.1.0-SNAPSHOT"
  :description "A Clojure client for InfluxDB"
  :url         "https://github.com/olauzon/capacitor"
  :license {
    :name "Eclipse Public License"
    :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure "1.5.1"]
    [clj-http            "0.7.7"]
    [cheshire            "5.2.0"]]
  :plugins [[codox "0.6.6"]]
  :dev-dependencies [[codox "0.6.6"]]
  ;;:global-vars { *warn-on-reflection* true }
  :aot :all
  :main capacitor.core)
