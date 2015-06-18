(ns %s.main (:gen-class)
  (:require [clojure.java.jdbc :as sql]
            [clj-time.local :as local]
            [baotask.config :refer [config]]
            [clojure.set :as set]
            %s))

(def database
  {:subprotocol  (get-in config ["db" "subprotocol"])
   :subname  (str  (str "//" (get-in config ["db"  "host"]) ":" (get-in config ["db"  "port"]) "/" (get-in config ["db" "db-name"]) (if (= "mysql" (get-in config ["db" "subprotocol"])) "?useUnicode=true&characterEncoding=UTF-8" "")))
   :user (get-in config ["db" "user"])
   :password (get-in config ["db" "password"])})

(defn completed-migrations []
  "获取已完成的脚本"
  (->> (sql/query database
                  ["SELECT name FROM migrations ORDER BY name DESC"])
       (map #(:name %%))
       vec))

(defn -main[& args]
  (try
    (println "开始执行脚本!")
    (sql/db-do-commands database "CREATE TABLE if not exists migrations (
                                        name character varying(100) NOT NULL DEFAULT ''
                                      );")
    (doseq [mig (sort (set/difference %s
                                    (set (completed-migrations))))]
      (try
        (sql/db-do-commands database (format "insert into  migrations(name) values('%%s')" mig))
        (println (str "正在运行" mig "脚本！"))
        ((resolve (symbol (str "%s.migrations." (first (clojure.string/split mig #"\.")) "/up"))))
        (println (str mig "脚本！运行结束"))
        (catch Exception e (.printStackTrace e)
                           (println (str mig "脚本出现了异常！"))
                           (sql/delete! database :migrations ["name=?" mig])
                           (throw (Exception. "出现了异常")))))
    (catch Exception e (.printStackTrace e)
                       (println "出现异常退出本执行")
                       (System/exit 1))))
