(ns rui.mig.mig-test
  (:require [clojure.test :refer :all]
            [rui.mig.mig :as mig]))




(deftest rui-mig-test
  (testing "test rui mig"
    (is (= ["rui.migrations.mxxxx"]) (mig/mig-list-to-require-string #{"mxxxx.clj"} {:group "rui"}))))


