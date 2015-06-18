(ns rui.mig.mig
  (:require [clojure.set :as set]
            [bultitude.core :as b]
            [clojure.java.jdbc :as sql]
            [clojure.java.io :as  io]
            [clojure.data.json :as json]
            [me.raynes.fs :as fs]))


(def config
 (json/read-str (slurp "./config.json")))

(defn init-dictory [path]
  (if-not (fs/directory? path)
    (fs/mkdir path)))


(defn get-source-dir [project-name]
  (clojure.string/join "_" (clojure.string/split project-name #"-")))

(defmacro fmt [^String string]
  (let [-re #"#\{(.*?)\}"
        fstr (clojure.string/replace string -re "%s")
        fargs (map #(read-string (second %)) (re-seq -re string))]
    `(format ~fstr ~@fargs)))

(defn get-database
  "fetch database connection info given project.clj options hash"
  []
   {:subprotocol  (get-in config ["db" "subprotocol"])
    :subname  (str  (str "//" (get-in config ["db"  "host"]) ":" (get-in config ["db"  "port"]) "/" (get-in config ["db" "db-name"]) (if (= "mysql" (get-in config ["db" "subprotocol"])) "?useUnicode=true&characterEncoding=UTF-8" "")))
    :classname (if (= "mysql" (get-in config ["db" "subprotocol"]))
                  "com.mysql.jdbc.Driver"
                  "org.postgresql.Driver")
   :user (get-in config ["db" "user"])
   :password (get-in config ["db" "password"])})


(defn get-migration-files
  ([project] (get-migration-files (format "./src/%s/migrations" (get-source-dir (:group project)))))
  ([project dir-name] (->> (io/file dir-name)
                   (.listFiles)
                   (map #(.getName %))
                   sort)))

(defn completed-migrations []
  (->> (sql/query (get-database)
                  ["SELECT name FROM migrations ORDER BY name DESC"])
       (map #(:name %))
       vec))


(defn run-migrations [files  project]
  (let [mig-db (get-database)]
    (try
      (doseq [file files]
        (try
          (load-file (format "./src/%s/migrations/%s" (get-source-dir (:group project)) file))
          (let [migr-id file
                symbole-file (format "%s.migrations.%s/up" (get-source-dir (:group project)) (first (clojure.string/split file #"\.")))]
            
            (println (str "Migrating: " file))
            (sql/db-do-commands (get-database) (format "insert into  migrations(name) values('%s')"  file))
            (println (str "It's runing the " file " script！"))
            ((resolve (symbol symbole-file)))
          
            (println (str file " script！end of run.")))
          (catch Exception e (.printStackTrace e)
                               (do
                                 (println "faill")
                                 (println file)
                                 (sql/delete! mig-db :migrations ["name=?" file])
                                 (throw (Exception. "It had a exception"))))))
      (catch Exception e (.printStackTrace e)
                         (println "THE Exception is quit！")))))


(defn get-mig-list[project]
  "Geting all scripts"
  
  (set (get-migration-files project (format "./src/%s/migrations" (get-source-dir (:group project))))))

(defn mig-list-to-require-string [mig-lsit project]
  (map #(format "[%s.migrations.%s]" (get-source-dir (:group project)) (first (clojure.string/split % #"\."))) mig-lsit))

(defn get-uncompleted [project]
  "Geting all not run script"
  (sort (set/difference (get-mig-list project)
                        (set (completed-migrations)))))



(defn migrate [project ]
  (try
    (println "start migratetions")
    (sql/db-do-commands (get-database) "CREATE TABLE if not exists migrations (
                                        name character varying(100) NOT NULL DEFAULT ''
                                      );")
    (run-migrations (get-uncompleted project)   project)
    (catch Exception e (.printStackTrace e))))


