(ns %s.main (:gen-class)
  (:require [clojure.java.jdbc :as sql]
            [clojure.set :as set]
            [clojure.data.json :as json]
            %s))
   (def config
                (json/read-str (slurp "./config.json")))
(def database
  {:subprotocol  (get-in config ["db" "subprotocol"])
   :subname  (str  (str "//" (get-in config ["db"  "host"]) ":" (get-in config ["db"  "port"]) "/" (get-in config ["db" "db-name"]) (if (= "mysql" (get-in config ["db" "subprotocol"])) "?useUnicode=true&characterEncoding=UTF-8" "")))
   :user (get-in config ["db" "user"])
   :password (get-in config ["db" "password"])})

(defn completed-migrations []
  "Get all   script is  not over"
  (->> (sql/query database
                  ["SELECT name FROM migrations ORDER BY name DESC"])
       (map #(:name %%))
       vec))

(defn -main[& args]
  (try
    (println "Start execute  scrpt!")
    (sql/db-do-commands database "CREATE TABLE if not exists migrations (
                                        name character varying(100) NOT NULL DEFAULT ''
                                      );")
    (doseq [mig (sort (set/difference %s
                                    (set (completed-migrations))))]
      (try
        (sql/db-do-commands database (format "insert into  migrations(name) values('%%s')" mig))
        (println (str "It's runing the " mig " ！"))
        ((resolve (symbol (str "%s.migrations." (first (clojure.string/split mig #"\.")) "/up"))))
        (println (str mig " script is over"))
        (catch Exception e (.printStackTrace e)
                           (println (str mig " is have a error！"))
                           (sql/delete! database :migrations ["name=?" mig])
                           (throw (Exception. "It   had a error")))))
    (catch Exception e (.printStackTrace e)
                       (println "It had a error")
                       (System/exit 1))))
