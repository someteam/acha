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
     :merge false,
     :author "Andy Warhol",
     :id "28736ac09b8d82c9075e8b69b60590edfffba74b",
     :changed-files [],
     :message "Test commit"}
    opts))

(deftest date-scanner
  (let [ach (commit-info :time #inst "2014-04-20T17:29:59.000-00:00")]
    (is ((second (achievement/make-date-scanner [:foo 4 20])) ach))))

(deftest language-scanner
  (let [time #inst "2014-04-20T17:29:59.000-00:00"
        diffs [[:added]]]
    (is (= {:time time
            :username "Bender"}
          ((last (achievement/make-language-scanner [:clojure ["clj"]]))
           {:time time
            :author "Bender"
            :changed-files [{:kind :add
                             :new-file {:path "project.clj"}}]})))))

(deftest easy-fix
  (testing "nonadjacent lines"
    (let [ach (commit-info :changed-files
                [{:kind :edit
                  :diff [{:added [["line a" 10]], :removed [["line b" 15]]}
                         {:added [["line b" 10]], :removed [["line a" 15]]}]}])]
      (is ((second achievement/easy-fix) ach))))
  (testing "adjacent lines"
    (let [ach (commit-info :changed-files
                [{:kind :edit
                  :diff [{:added [["line a" 10]], :removed [["line a" 11]]}]}])]
      (is ((second achievement/easy-fix) ach)))))

(deftest mover
  (let [ach (commit-info :changed-files [{:kind :rename}])]
    (is ((second achievement/mover) ach))))

(deftest fat-ass
  (let [ach (commit-info :changed-files [{:kind :add
                                          :new-file {:type :binary
                                                     :size 3000000}}])]
    (is ((second achievement/fat-ass) ach))))

(deftest world-balance
  (testing "balance in many files"
    (let [ach (commit-info :changed-files
                           [{:kind :edit, :loc {:added 13 :removed 17}}
                            {:kind :edit, :loc {:added 17 :removed 13}}])]
      (is ((second achievement/world-balance) ach))))
  (testing "balance in one file"
    (let [ach (commit-info :changed-files
                           [{:kind :edit, :loc {:added 10 :removed 10}}])]
      (is (nil? ((second achievement/world-balance) ach))))))

(deftest massive
  (let [ach (commit-info :changed-files
                         [{:kind :edit, :loc {:added 2000 :removed 10}}])]
    (is ((second achievement/massive) ach))))

(deftest eraser
  (testing "with additions"
    (let [ach (commit-info :changed-files
                           [{:kind :edit, :loc {:added 200 :removed 10}}])]
      (is (nil? ((second achievement/eraser) ach)))))
  (testing "without additions"
    (let [ach (commit-info :changed-files
                           [{:kind :edit, :loc {:added 0 :removed 10}}])]
      (is ((second achievement/eraser) ach)))))

