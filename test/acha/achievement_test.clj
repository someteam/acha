(ns acha.achievement-test
  (:require [clojure.test :refer :all]
            [acha.achievement :as achievement]))

(deftest date-scanner
  (let [time #inst "2014-04-20T17:29:59.000-00:00"]
    (is (= {:name "foo"
            :time time
            :username "Bender"}
          ((achievement/date-scanner "foo" 3 20)
           {:time time
            :author "Bender"
            })))))
