(ns acha.achievement-test
  (:require [clojure.test :refer :all]
            [acha.achievement :as achievement]))

(deftest date-scanner
  (let [time #inst "2014-04-20T17:29:59.000-00:00"]
    (is (= {:time time
            :username "Bender"}
          ((last (achievement/make-date-scanner [:foo 3 20]))
           {:time time
            :author "Bender"
            })))))

(deftest language-scanner
  (let [time #inst "2014-04-20T17:29:59.000-00:00"
        diffs [[:added]]]
    (is (= {:time time
            :username "Bender"}
          ((last (achievement/make-language-scanner [:clojure ["clj"]]))
           {:time time
            :author "Bender"
            :changed-files [[:add {:id 123} {:id 345 :path "project.clj"}]]})))))
