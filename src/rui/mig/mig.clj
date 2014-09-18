(ns rui.mig.mig
  (:require [clojure.set :as set]
            [bultitude.core :as b]
            [clojure.java.jdbc :as sql]
            [clojure.java.io :as  io]
            [me.raynes.fs :as fs]))

(defn init-dictory [path]
  (if-not (fs/directory? path)
    (fs/mkdir path)))

(defmacro fmt [^String string]
  (let [-re #"#\{(.*?)\}"
        fstr (clojure.string/replace string -re "%s")
        fargs (map #(read-string (second %)) (re-seq -re string))]
    `(format ~fstr ~@fargs)))

(defn get-database
  "fetch database connection info given project.clj options hash"
  [db-name]
  (let [db-name (keyword (str db-name "db"))]
    (-> "./config.json" slurp read-string db-name)))


(defn get-migration-files
  ([project] (get-migration-files (format "./src/%s/migrations" (:group project))))
  ([project dir-name] (->> (io/file dir-name)
                   (.listFiles)
                   (map #(.getName %))
                   sort)))

(defn completed-migrations []
  (->> (sql/query (get-database "admin")
                  ["SELECT name FROM rui_migrations ORDER BY name DESC"])
       (map #(:name %))
       vec))


(defn run-migrations [files direction project]
  (let [mig-db (get-database "admin")]
    (try
      (doseq [file files]
        (try
          (load-file (format "./src/%s/migrations/%s" (:group project) file))
          (let [migr-id file
                symbole-file (format "%s.migrations.%s/%s" (:group project) (first (clojure.string/split file #"\.")) (name direction))]
            (if (= direction 'down)
              (do (println (str "Reversing: " file))
                  (sql/delete! mig-db :rui_migrations ["name=?" migr-id])
                  )
              (do (println (str "Migrating: " file))
                  (sql/db-do-commands (get-database "admin") (format "insert into  rui_migrations(name, start_time) values('%s', now())" file))
                  (println (str "正在运行" file "脚本！"))))
            ((resolve (symbol symbole-file)))
            (sql/db-do-commands (get-database "admin") (format "update rui_migrations set end_time=now() where name='%s'" file))
            (println (str file "脚本！运行结束")))
          (catch Exception e (.printStackTrace e)
                             (if (= direction 'down)
                               (sql/insert! mig-db :rui_migrations  {:name file})
                               (do
                                 (println "faill")
                                 (println file)
                                 (sql/delete! mig-db :rui_migrations ["name=?" file])
                                 (throw (Exception. "出现了异常")))))))
      (catch Exception e (.printStackTrace e)
                         (println "退出异常")))))


(defn get-mig-list[project]
  "获取所有数据变更脚本"
  (set (get-migration-files project (format "./src/%s/migrations" (:group project)))))

(defn mig-list-to-require-string [mig-lsit project]
  (map #(format "[%s.migrations.%s]" (:group project) (first (clojure.string/split % #"\."))) mig-lsit))

(defn get-uncompleted [project]
  "获取未完成的脚本"
  (sort (set/difference (get-mig-list project)
                        (set (completed-migrations)))))



(defn migrate [project]
  (try
    (println "start migratetions")
    (sql/db-do-commands (get-database "admin") "CREATE TABLE if not exists `rui_migrations` (
                                                `name` varchar(100) NOT NULL DEFAULT '',
                                                `start_time` datetime DEFAULT NULL,
                                                `end_time` datetime DEFAULT NULL,
                                                UNIQUE KEY `name` (`name`)
                                              ) ENGINE=InnoDB DEFAULT CHARSET=utf8;")
    (run-migrations (get-uncompleted project)  'up project)
    (catch Exception e (.printStackTrace e))))

(defn rollback [n]
  (let [n (Long. (or n 1))]
    (run-migrations  (take n (completed-migrations ))  'down)))
