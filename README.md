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
:dependencies [[capacitor "0.1.0-SNAPSHOT"]]
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
    {:email "john@gmail.com"}
    {:email "john@yahoo.com"}
    {:email "john@hotmail.com"}
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
(def query-00
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(10m) "
    "WHERE email =~ /.*gmail\\.com/"))

(def query-01
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(1m)"))
```

### Submit queries

```clj
(get-query c query-00)
```

Returns:

```clj
[{:name "logins", :count 3, :sequence_number 1, :time 1384662000000}
 {:name "logins", :count 3, :sequence_number 1, :time 1384661400000}]
```

```clj
(get-query c query-01)
```

Returns:

```clj
[{:name "logins", :count 7, :sequence_number 1, :time 1384662540000}
 {:name "logins", :count 7, :sequence_number 1, :time 1384661760000}]
```

See [examples/basic.clj](https://github.com/olauzon/capacitor/blob/master/examples/basic.clj) for these examples in one file.


Async API
---------

Capacitor has an asynchronous API for event batch accumulation and submission.

```clj
(use '[capacitor.core])
(use '[capacitor.async])
```

Define an InfluxDB client (see basic.clj for example)
```clj
(def c
  (make-client {
    :db       "my-new-db"
    :username "myuser"
    :password "mypassword" }))
```

Make a channel to buffer incoming events
```clj
(def events-in (atom (make-chan)))
```

Make a channel to collect post responses
```clj
(def resp-out (atom (make-chan)))
```

Start the run loop with a batch size of max 10 events and max 5 seconds
```clj
(run! @events-in @resp-out c 10 5000)
```

Enqueue events
```clj
(enqueue @events-in {:email "paul@gmail.com"
                     :series "logins"})

(enqueue @events-in {:email "john@gmail.com"
                     :series "signups"})

(enqueue @events-in {:email "ringo@gmail.com"
                     :series "logins"})

(enqueue @events-in {:email "george@gmail.com"
                     :series "logins"})

(enqueue @events-in {:email "syd@hotmail.com"
                     :series "signups"})

(enqueue @events-in {:email "roger@hotmail.com"
                     :series "logins"})

(enqueue @events-in {:email "nick@hotmail.com"
                     :series "logins"})

(enqueue @events-in {:email "rick@hotmail.com"
                     :series "logins"})

(enqueue @events-in {:email "david@hotmail.com"
                     :series "logins"})

(enqueue @events-in {:email "sting@yahoo.com"
                     :series "signups"})
```

Query the db
```clj
(def query-00
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(1s)"))

(get-query c query-00)
```


See [examples/async.clj](https://github.com/olauzon/capacitor/blob/master/examples/async.clj) for these examples in one file.

## License

Copyright © 2013 Olivier Lauzon

Distributed under the Eclipse Public License.
