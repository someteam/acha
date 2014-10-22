(ns acha.achievement-test
  (:require [clojure.test :refer :all]
            [acha.achievement :as achievement])
  (:import [java.util TimeZone])
  )

(defn commit-info [& {:as opts}]
  (merge
    {:between-time -1411450912411,
     :email "warhol@acha-acha.co",
     :timezone (TimeZone/getTimeZone "CST")
     :time #inst "2014-10-09T14:09:38.000-00:00"
     :parents-count 1,
     :author "Andy Warhol",
     :id "28736ac09b8d82c9075e8b69b60590edfffba74b",
     :changed-files [],
     :message "Test commit"}
    opts))

(deftest date-scanner
  (let [ach (commit-info :time #inst "2014-04-01T17:29:59.000-00:00")]
    (is ((get-in achievement/base [:commit-scanners :fools-day]) ach))))

(deftest language-scanner
  (is ((get-in achievement/base [:commit-scanners :clojure])
         (commit-info 
           :changed-files [{:kind :add, :new-file {:path "project.clj"}}]))))

(deftest easy-fix
  (testing "nonadjacent lines"
    (let [ach (commit-info :changed-files
                [{:kind :edit
                  :diff [{:added [["line a" 10]], :removed [["line b" 15]]}
                         {:added [["line b" 10]], :removed [["line a" 15]]}]}])]
      (is ((get-in achievement/base [:commit-scanners :easy-fix]) ach))))
  (testing "adjacent lines"
    (let [ach (commit-info :changed-files
                [{:kind :edit
                  :diff [{:added [["line a" 10]], :removed [["line a" 11]]}]}])]
      (is ((get-in achievement/base [:commit-scanners :easy-fix]) ach)))))

(deftest mover
  (let [ach (commit-info :changed-files [{:kind :rename}])]
    (is ((get-in achievement/base [:commit-scanners :mover]) ach))))

(deftest fat-ass
  (let [ach (commit-info :changed-files [{:kind :add
                                          :new-file {:type :binary
                                                     :size 3000000}}])]
    (is ((get-in achievement/base [:commit-scanners :fat-ass]) ach))))

(deftest world-balance
  (testing "balance in many files"
    (let [ach (commit-info :changed-files
                           [{:kind :edit, :loc {:added 13 :removed 17}}
                            {:kind :edit, :loc {:added 17 :removed 13}}])]
      (is ((get-in achievement/base [:commit-scanners :world-balance]) ach))))
  (testing "balance in one file"
    (let [ach (commit-info :changed-files
                           [{:kind :edit, :loc {:added 10 :removed 10}}])]
      (is (nil? ((get-in achievement/base [:commit-scanners :world-balance]) ach))))))

(deftest massive
  (let [ach (commit-info :changed-files
                         [{:kind :edit, :loc {:added 2000 :removed 10}}])]
    (is ((get-in achievement/base [:commit-scanners :massive]) ach))))

(deftest eraser
  (testing "with additions"
    (let [ach (commit-info :changed-files
                           [{:kind :edit, :loc {:added 200 :removed 10}}])]
      (is (nil? ((get-in achievement/base [:commit-scanners :eraser]) ach)))))
  (testing "without additions"
    (let [ach (commit-info :changed-files
                           [{:kind :edit, :loc {:added 0 :removed 10}}])]
      (is ((get-in achievement/base [:commit-scanners :eraser]) ach)))))

(deftest citation-needed
  (testing "link"
    (let [ach (commit-info :message "See for more detilas http://stackoverflow.com/questions/701166/can-you-provide-some-examples-of-why-it-is-hard-to-parse-xml-and-html-with-a-reg")]
      (is ((get-in achievement/base [:commit-scanners :citation-needed]) ach))))
  (testing "without"
    (let [ach (commit-info :message "bla-bla-bla")]
      (is (not ((get-in achievement/base [:commit-scanners :citation-needed]) ach))))))

(deftest holy-war
  (testing "true case"
    (let [ach (commit-info :changed-files
                [{:kind :edit
                  :diff [{:added [["  line a" 10]], :removed [["	line a" 15]]}]}])]
      (is ((get-in achievement/base [:commit-scanners :holy-war]) ach))))
  (testing "false case"
    (let [ach (commit-info :changed-files
                [{:kind :edit
                  :diff [{:added [["  line a" 10]], :removed [["  line a 	" 15]]}]}])]
      (is (not ((get-in achievement/base [:commit-scanners :holy-war]) ach))))))

(deftest ocd
  (let [ach (commit-info :changed-files
              [{:kind :edit
                :diff [{:added [["  line a" 10]], :removed [["  line a  " 15]]}]}])]
    (is ((get-in achievement/base [:commit-scanners :ocd]) ach))))

(deftest multi-lingua
  (testing "3 main languages"
    (is (= ((get-in achievement/base [:commit-scanners :multilingual])
              (commit-info
                :changed-files [{:kind :add, :new-file {:path "project.clj"}}
                                {:kind :edit, :new-file {:path "project.java"}}
                                {:kind :edit, :new-file {:path "react.js"}}]))
           {:level 1})))
  (testing "2 main languages and xml"
    (is (nil? ((get-in achievement/base [:commit-scanners :multilingual])
                 (commit-info
                   :changed-files [{:kind :add, :new-file {:path "project.clj"}}
                                   {:kind :edit, :new-file {:path "project.java"}}
                                   {:kind :edit, :new-file {:path "old.xml"}}]))))))


