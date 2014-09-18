(ns leiningen.rui-mig
  (:gen-class)
  (:use [clj-time.format]
        [clj-time.local])
  (:require [rui.mig.mig :as migrate]
            [clojure.java.io :as io]
            [baotask.config :as config]))


(def custom-formatter (formatter-local "yyyyMMddHHmmss"))

(defn create-template [name text]
  ;创建模板
  (spit (format "%s.clj" name) text))

(def template-create
  "(ns rui.migrations.%s
    (:require [clojure.java.jdbc :as sql]))
  
    (defn execute-in-db! [ & queries]
      (doseq [q queries]
        (sql/execute! {:subprotocol  (get-in (config/config) ["db" "subprotocol"])
                       :subname  (get-in (config/config) ["db" "db-name"])
                       :user (get-in (config/config) ["db" "user"])
                       :password (get-in (config/config) ["db" "paswrod"])} (if (string? q) [q] q))))

    (defn up[]
      (execute-in-db!  ))

    (defn down[]
      )"
  )

(defn deploy [project]
         (let [path (format "./src/%s/main" (:group project))
               mig-list (migrate/get-mig-list project)]
           (create-template path
                            (format (-> "deploy.clj" io/resource slurp)
                                    (:group project)
                                    (clojure.string/join "\n" (migrate/mig-list-to-require-string mig-list project))
                                    (str mig-list)
                                    (:group project)))
           (println (format "创建了数据库部署脚本%s.clj" path))))

(defn rui-mig
                                        ;插件执行入口
  ([project command]

     (let [opts (:clj-sql-up project)
           path (str "./src/" (:group project) "/migrations/")]
       (migrate/init-dictory path)
       (cond
        (= command "create") (let [name (str "m" (unparse custom-formatter (local-now)))]
                               (create-template (str path name)
                                                (format template-create name))
                               (println (format "创建了数据库变更脚本文件%s.clj" name)))
        (= command "deploy") (deploy project)
        )))
  ([project]
     (let [opts (:clj-sql-up project)]
       (migrate/migrate project))))
