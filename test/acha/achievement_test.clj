(ns acha.achievement-test
  (:require [clojure.test :refer :all]
            [acha.util :as util]
            [acha.achievement :as achievement])
  (:import [java.util TimeZone Calendar])
  )

(defn commit-info [& {:as opts}]
  (merge
    {:between-time -1411450912411,
     :email "warhol@acha-acha.co",
     :calendar (util/create-calendar #inst "2014-10-09")
     :parents ["parent-id"],
     :author "Andy Warhol",
     :id "28736ac09b8d82c9075e8b69b60590edfffba74b",
     :changed-files [],
     :message "Test commit"}
    opts))

(deftest date-scanner
  (let [commit (commit-info :calendar (util/create-calendar #inst "2014-04-01"))]
    (is ((get-in achievement/base [:commit-scanners :fools-day]) commit))))

(deftest language-scanner
  (is ((get-in achievement/base [:commit-scanners :clojure])
         (commit-info 
           :changed-files [{:kind :add, :new-file {:path "project.clj"}}]))))

(deftest easy-fix
  (testing "nonadjacent lines"
    (let [changed-file {:kind :edit
                        :diff [{:added [["line a" 10]], :removed [["line b" 15]]}
                               {:added [["line b" 10]], :removed [["line a" 15]]}]}
          commit (commit-info :changed-files [changed-file])]
      (is ((get-in achievement/base [:diff-scanners :easy-fix]) commit changed-file))))
  (testing "adjacent lines"
    (let [changed-file {:kind :edit :diff [{:added [["line a" 10]], :removed [["line a" 11]]}]}
          commit (commit-info :changed-files [changed-file])]
      (is ((get-in achievement/base [:diff-scanners :easy-fix]) commit changed-file)))))

(deftest mover
  (let [changed-file {:kind :rename}
        commit (commit-info :changed-files [changed-file])]
    (is ((get-in achievement/base [:diff-scanners :mover]) commit changed-file))))

(deftest fat-ass
  (let [changed-file {:kind :add :new-file {:type :binary :size 3000000}}
        commit (commit-info :changed-files [changed-file])]
    (is ((get-in achievement/base [:diff-scanners :fat-ass]) commit changed-file))))

(deftest world-balance
  (testing "balance in many files"
    (let [commit (commit-info :changed-files
                           [{:kind :edit, :loc {:added 13 :removed 17}}
                            {:kind :edit, :loc {:added 17 :removed 13}}])]
      (is ((get-in achievement/base [:commit-scanners :world-balance]) commit))))
  (testing "balance in one file"
    (let [commit (commit-info :changed-files
                           [{:kind :edit, :loc {:added 10 :removed 10}}])]
      (is (nil? ((get-in achievement/base [:commit-scanners :world-balance]) commit))))))

(deftest massive
  (let [commit (commit-info :changed-files
                         [{:kind :edit, :loc {:added 2000 :removed 10}}])]
    (is ((get-in achievement/base [:commit-scanners :massive]) commit))))

(deftest eraser
  (testing "with additions"
    (let [commit (commit-info :changed-files
                           [{:kind :edit, :loc {:added 200 :removed 10}}])]
      (is (nil? ((get-in achievement/base [:commit-scanners :eraser]) commit)))))
  (testing "without additions"
    (let [commit (commit-info :changed-files
                           [{:kind :edit, :loc {:added 0 :removed 10}}])]
      (is ((get-in achievement/base [:commit-scanners :eraser]) commit)))))

(deftest citation-needed
  (testing "link"
    (let [commit (commit-info :message "See for more detilas http://stackoverflow.com/questions/701166/can-you-provide-some-examples-of-why-it-is-hard-to-parse-xml-and-html-with-a-reg")]
      (is ((get-in achievement/base [:commit-scanners :citation-needed]) commit))))
  (testing "without"
    (let [commit (commit-info :message "bla-bla-bla")]
      (is (not ((get-in achievement/base [:commit-scanners :citation-needed]) commit))))))

(deftest holy-war
  (testing "true case"
    (let [changed-file {:kind :edit :diff [{:added [["  line a" 10]], :removed [["	line a" 15]]}]}
          commit (commit-info :changed-files [changed-file])]
      (is ((get-in achievement/base [:diff-scanners :holy-war]) commit changed-file))))
  (testing "false case"
    (let [changed-file {:kind :edit :diff [{:added [["  line a" 10]], :removed [["  line a 	" 15]]}]}
          commit (commit-info :changed-files [changed-file])]
      (is (not ((get-in achievement/base [:diff-scanners :holy-war]) commit changed-file))))))

(deftest ocd
  (let [changed-file {:kind :edit :diff [{:added [["  line a" 10]], :removed [["  line a  " 15]]}]}
        commit (commit-info :changed-files [changed-file])]
    (is ((get-in achievement/base [:diff-scanners :ocd]) commit changed-file))))

(deftest multi-lingua
  (testing "3 main languages"
    (let [commit (commit-info
                   :changed-files [{:kind :add, :new-file {:path "project.clj"}}
                                   {:kind :edit, :new-file {:path "project.java"}}
                                   {:kind :edit, :new-file {:path "react.js"}}])]
      (is (= ((get-in achievement/base [:commit-scanners :multilingual]) commit)
             [{:level 1 :commit-info commit}]))))
  (testing "2 main languages and xml"
    (is (nil? ((get-in achievement/base [:commit-scanners :multilingual])
                 (commit-info
                   :changed-files [{:kind :add, :new-file {:path "project.clj"}}
                                   {:kind :edit, :new-file {:path "project.java"}}
                                   {:kind :edit, :new-file {:path "old.xml"}}]))))))

(deftest catchphrase 
  (testing "false case"
    (is (empty? ((get-in achievement/base [:timeline-scanners :catchphrase])
                  [(commit-info :calendar (util/create-calendar #inst "2014-04-01"))
                   (commit-info :calendar (util/create-calendar #inst "2014-04-02"))
                   (commit-info :calendar (util/create-calendar #inst "2014-04-03"))]
                  :threshold 4))))
  (testing "true case"
    (is (= [{:commit-info (commit-info :calendar (util/create-calendar #inst "2014-04-01"))}]
           ((get-in achievement/base [:timeline-scanners :catchphrase])
                 [(commit-info :calendar (util/create-calendar #inst "2014-04-01"))
                  (commit-info :calendar (util/create-calendar #inst "2014-04-02"))
                  (commit-info :calendar (util/create-calendar #inst "2014-04-03"))]
                 :threshold 3)))))

(deftest loneliness
  (testing "false case"
    (is (empty? ((get-in achievement/base [:timeline-scanners :loneliness])
                  [(commit-info :email "foo@bar" :calendar (util/create-calendar #inst "2014-04-01"))
                   (commit-info :email "boo@far" :calendar (util/create-calendar #inst "2014-04-02"))]))))
  (testing "true case"
    (is (= [{:commit-info (commit-info :calendar (util/create-calendar #inst "2014-04-01"))}]
           ((get-in achievement/base [:timeline-scanners :loneliness])
                   [(commit-info :calendar (util/create-calendar #inst "2014-04-01"))
                    (commit-info :calendar (util/create-calendar #inst "2014-04-02"))]))))
  (testing "skip last month"
    (is (empty? ((get-in achievement/base [:timeline-scanners :loneliness])
                  [(commit-info :calendar (Calendar/getInstance))
                   (commit-info :calendar (Calendar/getInstance))])))))

(deftest necromancer
  (testing "false case"
    (is (empty? ((get-in achievement/base [:timeline-scanners :necromancer])
                  [(commit-info :calendar (util/create-calendar #inst "2014-03-08"))
                   (commit-info :calendar (util/create-calendar #inst "2014-04-07"))]))))
  (testing "true case"
    (is (= [{:commit-info (commit-info :calendar (util/create-calendar #inst "2014-04-08"))}]
           ((get-in achievement/base [:timeline-scanners :necromancer])
                   [(commit-info :calendar (util/create-calendar #inst "2014-03-07"))
                    (commit-info :calendar (util/create-calendar #inst "2014-04-08"))])))))




