# rui-mig
A relationship of database migration plugin on the lein 
## Support 
PostgreSQL and MySQL
## Usage

__Leiningen__ ([via Clojars](https://clojars.org/rui-mig))

Put one of the following into the `:plugins` vector of the `:user` profile in your `~/.lein/profiles.clj`:

[![Clojars Project](http://clojars.org/rui-mig/latest-version.svg)](http://clojars.org/rui-mig)

At first please create a file named config.json to store database infomation e.g
MySQL
```javascript
{
  "db": {
    "db-name": "eg",
    "user": "root",
    "host": "127.0.0.1",
    "port": "3306",
    "password": "123",
    "subprotocol": "mysql"
  }
}  
```
PostgreSQL
```javascript
{
  "db": {
    "db-name": "eg",
    "user": "postgres",
    "host": "localhost",
    "port": "5433",
    "password": "123456",
    "subprotocol": "postgresql"
  }
}
```


If you want add a migration:

```
lein rui-mig create
```

It will be generat a script of src/rui/migrations/dataetimexxxx.clj  and it's content like follow
```clj
(ns projectname.migrations.m20150618164213
            (:require [clojure.java.jdbc :as sql]
                      [clojure.data.json :as json]))
              (def config
                (json/read-str (slurp "./config.json")))

            (def database
                {:subprotocol  (get-in config ["db" "subprotocol"])
                 :subname  (str  (str "//" (get-in config ["db"  "host"]) ":" (get-in config ["db"  "port"]) "/" (get-in config ["db" "db-name"]) (if (= "mysql" (get-in config ["db" "subprotocol"])) "?useUnicode=true&characterEncoding=UTF-8" "")))
                 :user (get-in config ["db" "user"])
                 :password (get-in config ["db" "password"])})
          
            (defn execute-in-db! [ & queries]
              (doseq [q queries]
                (sql/with-db-transaction [db database]
                                         (sql/execute! db   (if (string? q) [q] q)))))

            (defn up[]
              (execute-in-db! ""))
          


```
You can wright scripts of sql to the funtion of up or down 



If you want run migration:
```
lein rui-mig #Default to run the new migration

```
If you want deploy on jar (lein uberjar of the project to generate the file of jar)

At first you must be add follow to project.clj on :dependencies
```clojure
[org.clojure/java.jdbc "0.3.3"]
[org.clojure/data.json "0.2.4"]
[org.clojars.kbarber/postgresql "9.2-1002.jdbc4"] ;If your database is PostgreSQL
[mysql/mysql-connector-java "5.1.25"] ;If your database is MySQL

```

And then run
```
lein rui-mig deploy 
``` 
It will be generate the file of projectName.main.clj and add this projectName.main to project.clj :main

And then you run 
```
lein  uberjar
```
It will be generate a file of jar and you run 
```
 java -cp target/xxx.jar xxx.main  # To run migration by jar
```
It will be  run migration by file of jar


## Thansk

Thannks for [Xiang tao Zhou](https://github.com/taojoe) and [中瑞财富](http://www.zrcaifu.com/) for permission me to share this lein plugin.


