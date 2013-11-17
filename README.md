Capacitor  [![Build Status](https://travis-ci.org/olauzon/capacitor.png?branch=master)](https://travis-ci.org/olauzon/capacitor)
=========

A Clojure client for [InfluxDB](http://influxdb.org), a scalable open-source
time-series events and metrics database.

Follow the installation instructions on https://github.com/influxdb/influxdb to
build the latest development version of InfluxDB.


Installation
------------

### Leiningen

Capacitor is distributed via [Clojars](https://clojars.org/capacitor). Add the
following to your dependencies in `project.clj`:

```clj
:dependencies [[capacitor "0.0.1-SNAPSHOT"]]
```


Usage
-----

### Require in your app namespace

```clj
(ns my-app.core
  (:use [capacitor.core]))
```

### Configure a client

```clj
(def client
  (make-client {:db "my-new-db"}))
```

The `default-client` options are:

```clj
{  :host     "localhost"
   :scheme   "http"
   :port     8086
   :username "root"
   :password "root"
   :db       "default-db" }
```

### Create the configured database

```clj
(create-db client)
;=> 201
```

Returns HTTP status code `201` on success.

### Delete the configured database

```clj
(delete-db client)
;=> 204
```

Returns HTTP status code `204` on success.

### Create a database user

```clj
(create-db-user client "myuser" "mypassword")
;=> 200
```

Returns HTTP status code `200` on success.

### Configure a client for the database user

```clj
(def c
  (make-client {
    :db       "my-new-db"
    :username "myuser"
    :password "mypassword" }))
```

### Post time series data to the database

```clj
(post-points c "logins"
  [
    {:email "johh@gmail.com"}
    {:email "johh@yahoo.com"}
    {:email "johh@hotmail.com"}
    {:email "jill@gmail.com"}
    {:email "jason@gmail.com"}
    {:email "alice@yahoo.com"}
    {:email "bob@mac.com"} ])
;=> 200
```
Returns an HTTP status code `200` on success.
For this example, assume that some time passes and you resubmit the same query.


### Write SQLish queries

```clj
(def query-01
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(10m) "
    "WHERE email =~ /.*gmail\\.com/"))

(def query-02
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(1m)"))
```

### Submit queries

```clj
(get-query c query-01)
```

Returns:

```clj
[{:name "logins", :count 3, :sequence_number 1, :time 1384662000000}
 {:name "logins", :count 3, :sequence_number 1, :time 1384661400000}]
```

```clj
(get-query c query-02)
```

Returns:

```
[{:name "logins", :count 7, :sequence_number 1, :time 1384662540000}
 {:name "logins", :count 7, :sequence_number 1, :time 1384661760000}]
```

See `examples/basic.clj` for these examples in one file.


## License

Copyright © 2013 Olivier Lauzon

Distributed under the Eclipse Public License.
