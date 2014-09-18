(ns %s.main (:gen-class)
  (:require [rui-db.core :refer [get-db]]
            [clojure.java.jdbc :as sql]
            [clj-time.local :as local]
            [clojure.set :as set]
            %s))

(defn completed-migrations []
  "获取已完成的脚本"
  (->> (sql/query (get-db :admin)
                  ["SELECT name FROM rui_migrations ORDER BY name DESC"])
       (map #(:name %%))
       vec))

(defn -main[& args]
  (try
    (println "开始执行脚本!")
    (sql/db-do-commands (get-db :admin) "CREATE TABLE if not exists `rui_migrations` (
                                                `name` varchar(100) NOT NULL DEFAULT '',
                                                `start_time` datetime DEFAULT NULL,
                                                `end_time` datetime DEFAULT NULL,
                                                UNIQUE KEY `name` (`name`)
                                              ) ENGINE=InnoDB DEFAULT CHARSET=utf8;")
    (doseq [mig (sort (set/difference %s
                                    (set (completed-migrations))))]
      (try
        (sql/db-do-commands (get-db :admin) (format "insert into  rui_migrations(name, start_time) values('%%s', now())" mig))
        (println (str "正在运行" mig "脚本！"))
        ((resolve (symbol (str "%s.migrations." (first (clojure.string/split mig #"\.")) "/up"))))
        (sql/db-do-commands (get-db :admin) (format "update rui_migrations set end_time=now() where name='%%s'" mig))
        (println (str mig "脚本！运行结束"))
        (catch Exception e (.printStackTrace e)
                           (println (str mig "脚本出现了异常！"))
                           (sql/delete! (get-db :admin) :rui_migrations ["name=?" mig])
                           (throw (Exception. "出现了异常")))))
    (catch Exception e (.printStackTrace e)
                       (println "出现异常退出本执行")
                       (System/exit 1))))
