# rui-mig
A relationship of database migration plugin on lein 

## Usage

__Leiningen__ ([via Clojars](https://clojars.org/rui-mig))

Put one of the following into the `:plugins` vector of the `:user` profile in your `~/.lein/profiles.clj`:

[![Clojars Project](http://clojars.org/rui-mig/latest-version.svg)](http://clojars.org/rui-mig)

If you want add a migration:

```
lein rui-mig create
```

It will be generat a script of src/rui/migrations/dataetimexxxx.clj  and it's content like follow
```clj
(ns xxx.migrations.m20150104114822
  (:require 
            [baotask.storage :as st]))
              (def config
                (json/read-str (slurp "./config.json")))

            (def database
                {:subprotocol  (get-in config ["db" "subprotocol"])
                 :subname  (str  (str "//" (get-in config ["db"  "host"]) ":" (get-in config ["db"  "port"]) "/" (get-in config ["db" "db-name"]) (if (= "mysql" (get-in config ["db" "subprotocol"])) "?useUnicode=true&characterEncoding=UTF-8" "")))
                 :user (get-in config ["db" "user"])
                 :password (get-in config ["db" "password"])})
          
            (defn execute-in-db! [ & queries]
              (doseq [q queries]
                (sql/execute! database  (if (string? q) [q] q))))

            (defn up[]
              (execute-in-db! ""
                              ))

            (defn down[]
              (execute-in-db! ""
                              ))

```
You can wright scripts of sql to the funtion of up or down 



If you want run migration:
```
lein rui-mig #Default to run the function of up 
lein rui-mig up # To run the function of up 
lein rui-mig down # To run the function of down

```
If you want deploy on jar (lein uberjar of the project to generate the file of jar)

At first you must be run 
```
lein rui-mig deploy 
``` 
It will be generate the file of projectName.main.clj
And then you run 
```
lein  uberjar 
```
It will be generate a file of jar and you run 
```
 java -cp target/xxx.jar xxx.main up # up or down you will 
```
It will be  run migration by file of jar


## Thansk

Thannks for Xiang tao Zhou and 中瑞财富 for permission me to share this lein plugin.


