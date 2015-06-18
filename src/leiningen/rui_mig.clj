(ns leiningen.rui-mig
  (:gen-class)
  (:use [clj-time.format]
        [clj-time.local])
  (:require [rui.mig.mig :as migrate]
            [clojure.java.io :as io]))


(def custom-formatter (formatter-local "yyyyMMddHHmmss"))

(defn create-template [name text]
  ;创建模板
  (spit (format "%s.clj" name) text))

(defn template-create [project name]
  (format "(ns %s.migrations.%s
            (:require [clojure.java.jdbc :as sql]
                      [clojure.data.json :as json]))
              (def config
                (json/read-str (slurp \"./config.json\")))

            (def database
                {:subprotocol  (get-in config [\"db\" \"subprotocol\"])
                 :subname  (str  (str \"//\" (get-in config [\"db\"  \"host\"]) \":\" (get-in config [\"db\"  \"port\"]) \"/\" (get-in config [\"db\" \"db-name\"]) (if (= \"mysql\" (get-in config [\"db\" \"subprotocol\"])) \"?useUnicode=true&characterEncoding=UTF-8\" \"\")))
                 :user (get-in config [\"db\" \"user\"])
                 :password (get-in config [\"db\" \"password\"])})
          
            (defn execute-in-db! [ & queries]
              (doseq [q queries]
                (sql/with-db-transaction [db database]
                                         (sql/execute! db   (if (string? q) [q] q)))))

            (defn up[]
              (execute-in-db! \"\"))
          " (clojure.string/join "_" (clojure.string/split (:group project)  #"-"))  name))

(defn deploy [project]
         (let [ 
                _project-name (clojure.string/join "_" (clojure.string/split (:group project)  #"-"))
               path (format "./src/%s/main" _project-name)
               mig-list (migrate/get-mig-list project)]
           (create-template path
                            (format (-> "deploy.clj" io/resource slurp)
                                    _project-name 
                                    (clojure.string/join "\n" (migrate/mig-list-to-require-string mig-list project))
                                    (str mig-list)
                                    _project-name))
           (println (format "It had created a deploing of database script %s.clj" path))))

(defn rui-mig
  "It's run here start"                                      
  ([project command]
     (let [opts (:clj-sql-up project)
           project-name (:group project)
           _project-name (clojure.string/join "_" (clojure.string/split project-name #"-"))
           path (str "./src/" _project-name "/migrations/")]
       (migrate/init-dictory path)
       (cond
        (= command "create") 
          (let [name (str "m" (unparse custom-formatter (local-now)))]
            (create-template (str path name)
            (template-create project name))
            (println (format "It  had created a  script of migration   %s.clj" name)))
        (= command "deploy") 
         (deploy project)
        :else
         (migrate/migrate project)
        )))
  ([project]
       (migrate/migrate project)))