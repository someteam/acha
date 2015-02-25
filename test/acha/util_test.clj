(ns acha.util-test
  (:require [clojure.test :refer :all]
            [acha.util :as util]))

(deftest md5-test
  (testing "md5 hash generation"
    (is (= "6f8db599de986fab7a21625b7916589c" (util/md5 "test string")))))

(deftest normalize-uri-test
  (testing "normalize for web urls"
    (is (= "https://github.com/someteam/acha.git" (util/normalize-uri "https://githUb.com/someteam/acha.git"))))
  (testing "normalize for ssh urls"
    (is (= "git@github.com:someteam/acha.git" (util/normalize-uri "git@githUb.com:someteam/acha.git")))))

