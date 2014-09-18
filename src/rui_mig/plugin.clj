(ns rui-mig.plugin
  (:require [robert.hooke]
            [leiningen.install]
            [leiningen.rui-mig :as rui-mig]))

(defn auto-deploy [f & args]
  (let [{:keys [group] :as project} (first args)]
    (when group
      (rui-mig/deploy project)))
  (apply f args))

(defn hooks []
  (robert.hooke/add-hook #'leiningen.install/install
                         #'auto-deploy))
