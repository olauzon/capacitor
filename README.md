Capacitor  [![Build Status](https://travis-ci.org/olauzon/capacitor.png?branch=master)](https://travis-ci.org/olauzon/capacitor)
=========

A Clojure client for [InfluxDB](https://influxdata.com/time-series-platform/influxdb/).

Follow the [download instructions on influxdb.com]
(https://influxdata.com/downloads/) to install the latest version of InfluxDB.

**Support for InfluxDB v0.8.x is removed starting with v0.6.0.**

Installation
------------

### Leiningen

Capacitor is distributed via [Clojars](https://clojars.org/capacitor). Add the
following to your dependencies in `project.clj`:

```clj
:dependencies [[capacitor "0.6.0"]]
```


Usage
-----

### Require public functions

```clj
(use 'capacitor.core)
```

### Configure a client

```clj
(def client (make-client {:db "mynewdb"}))
```

The `default-client` options are:

```clj
{:host     "localhost"
 :scheme   "http"
 :port     8086
 :username "root"
 :password "root"
 :db       "testdb"
 :version  "0.9"}
```

### Get database server version

```clj
(version client)
;; => "0.11.0"
```

### Ping database server

```clj
(ping client)
;; => 2.479 (response time in ms)
```

### Create a database

```clj
(create-db client)
;; => true
```

### List databases

```clj
(list-dbs client)
;; => ("_internal" "mynewdb")
```

### Create a database user

```clj
(create-db-user client "myuser" "mypassword")
;; => true
```

### List database users

```clj
(list-db-users client)
;; => [{:columns ["user" "admin"], :values [["myuser" false]]}]
```

You can also zipmap columns with their values adding a `:results` key:

```clj
(list-db-users client true)
;; => ({:results ({:admin false, :user "myuser"})})
```

### Configure a client for the database user

```clj
(def c (make-client {:db "mynewdb" :username "myuser" :password "mypassword"}))
```

### Writing data points


A point can be a map containing the following keys:

```clj
(write-point c
  {:measurement "cpu_load"
   :tags        {"host" "1" "dc" 1}
   :fields      {"value" 1.1}
   :timestamp   1457624412})
```

Which can be shortened to a vector:

```clj
(write-point c
;; measurement tags                fields        timestamp
  ["cpu_load"  {"host" "1" "dc" 1} {"value" 1.2} 1457624413])
```

You can write multiple points at once with `write-points`:

```clj
(write-points c
  [{:measurement "cpu_load"
     :tags        {"host" "2" "dc" 1}
     :fields      {"value" 0.4}
     :timestamp   1457624412}
    {:measurement "cpu_load"
     :tags        {"host" "3" "dc" 1}
     :fields      {"value" 0.8}
     :timestamp   1457624412}])
```

Or use the vector form for points:

```clj
(write-points c
  [["cpu_load" {"host" "8" "dc" 2} {"value" 0.7} 1457624412]
   ["cpu_load" {"host" "9" "dc" 2} {"value" 0.5} 1457624412]])
```

### Write SQLish queries

```clj
(db-query c "SHOW SERIES" true)

(db-query c "SELECT * FROM cpu_load" true)

(db-query c "SELECT MAX(value) FROM cpu_load GROUP BY dc, host" true)
```


API Docs
--------

[API docs (codox)](http://olauzon.github.io/capacitor/docs/codox/index.html)


## Contributors

  - [@olauzon](https://github.com/olauzon)
  - [@pradeepchhetri](https://github.com/pradeepchhetri)
  - [@rborer](https://github.com/rborer)
  - [@jayp](https://github.com/jayp)
  - [@kfirmanty](https://github.com/kfirmanty)

## License

Copyright © 2013–2016 Olivier Lauzon

Distributed under the Eclipse Public License.
