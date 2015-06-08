Capacitor  [![Build Status](https://travis-ci.org/olauzon/capacitor.png?branch=master)](https://travis-ci.org/olauzon/capacitor)
=========

A Clojure client for [InfluxDB](http://influxdb.org), a scalable open-source
time-series events and metrics database.

Follow the [download instructions on influxdb.org]
(http://influxdb.org/download/) to install the latest version of InfluxDB.


Installation
------------

### Leiningen

Capacitor is distributed via [Clojars](https://clojars.org/capacitor). Add the
following to your dependencies in `project.clj`:

```clj
:dependencies [[capacitor "0.4.3"]]
```


Usage
-----

### Require in your app

```clj
(require '[capacitor.core :as influx])
```

### Configure a client

```clj
(def client
  (influx/make-client {:db "my-new-db"}))
```

The `default-client` options are:

```clj
{ :host     "localhost"
  :scheme   "http"
  :port     8086
  :username "root"
  :password "root"
  :db       "default-db" }
```

### Create the configured database

```clj
(influx/create-db client)
;=> 201
```

Returns HTTP status code `201` on success.

### Delete the configured database

```clj
(influx/delete-db client)
;=> 204
```

Returns HTTP status code `204` on success.

### Create a database user

```clj
(influx/create-db-user client "myuser" "mypassword")
;=> 200
```

Returns HTTP status code `200` on success.

### Configure a client for the database user

```clj
(def c
  (influx/make-client {
    :db       "my-new-db"
    :username "myuser"
    :password "mypassword" }))
```

### Post events to "logins" time series

```clj
(influx/post-points c "logins" [
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
(influx/get-query c query-00)
```

Returns:

```clj
[{:name "logins", :count 3, :sequence_number 1, :time 1384662000000}
 {:name "logins", :count 3, :sequence_number 1, :time 1384661400000}]
```

```clj
(influx/get-query c query-01)
```

Returns:

```clj
[{:name "logins", :count 7, :sequence_number 1, :time 1384662540000}
 {:name "logins", :count 7, :sequence_number 1, :time 1384661760000}]
```

See [examples/basic.clj]
(https://github.com/olauzon/capacitor/blob/master/examples/basic.clj)
for these examples in one file.


Async API
---------

Capacitor has an asynchronous API for event batch accumulation and submission.

### Require in your app

```clj
;; Base InfluxDB library
(require '[capacitor.core :as influx])

;; Async API
(require '[capacitor.async :as influx-async])
```

### Define an InfluxDB client

```clj
(def c
  (influx/make-client {
    :db       "my-new-db"
    :username "myuser"
    :password "mypassword" }))
```

### Make a channel to buffer incoming events

```clj
(def events-in (influx-async/make-chan))
```

### Make a channel to collect post responses

```clj
(def resp-out (influx-async/make-chan))
```

### Start the batch processing loop

With a batch size of max 10 events and max 5 seconds

```clj
(influx-async/run! events-in resp-out c 10 5000)
```

### Enqueue events

```clj
(influx-async/enqueue events-in {
  :series "logins"
  :email  "paul@gmail.com" })

(influx-async/enqueue events-in {
  :series "signups"
  :email  "john@gmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "ringo@gmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "george@gmail.com" })

(influx-async/enqueue events-in {
  :series "signups"
  :email  "syd@hotmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "roger@hotmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "nick@hotmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "rick@hotmail.com" })

(influx-async/enqueue events-in {
  :series "logins"
  :email  "david@hotmail.com" })

(influx-async/enqueue events-in {
  :series "signups"
  :email  "sting@yahoo.com" })

(dotimes [i 12]
  (influx-async/enqueue events-in {
    :series "logins"
    :email  (str "i" i "@i.com") }))
```

### Query the database

```clj
(def query-00
  (str
    "SELECT COUNT(email) "
    "FROM logins "
    "GROUP BY time(1s)"))

(influx/get-query c query-00)
```

### Close the `run!` loop

```clj
;; Require core.async
(require '[clojure.core.async :as async])

(async/close! events-in)
```


See [examples/async.clj]
(https://github.com/olauzon/capacitor/blob/master/examples/async.clj)
for these examples in one file.


API Docs
--------

[API docs (codox)](http://olauzon.github.io/capacitor/docs/codox/index.html)
|
[API docs (Marginalia)](http://olauzon.github.io/capacitor/docs/marg/index.html)


## Contributors

  - [@olauzon](https://github.com/olauzon)
  - [@pradeepchhetri](https://github.com/pradeepchhetri)
  - [@rborer](https://github.com/rborer)
  - [@jayp](https://github.com/jayp)

## License

Copyright © 2013–2014 Olivier Lauzon

Distributed under the Eclipse Public License.
