(ns acha.util-test
  (:require [clojure.test :refer :all]
            [acha.util :as util]))

(deftest md5-test
  (testing "md5 hash generation"
    (is (= "6f8db599de986fab7a21625b7916589c" (util/md5 "test string")))))

