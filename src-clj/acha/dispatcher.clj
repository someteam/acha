(ns acha.dispatcher
  (:require
    [clojure.tools.logging :as logging]
    [clojure.java.jdbc :as jdbc]
    [acha.achievement :as achievement]
    [acha.db :as db]
    [acha.util :as util]
    [acha.git-parser :as git-parser]))

(defn- scan-achievement [scanner commit-info]
  (try
    (scanner commit-info)
    (catch Exception e
      (logging/error e "Error occured during achievement scan commit"))))

(defn- analyze-commit [repo commit df]
  (try
    (let [commit-info (git-parser/commit-info repo commit df)]
      (->> (for [[code scanner] achievement/all-commit-info-scanners
                 :let [report (scan-achievement scanner commit-info)]
                 :when report]
             [[(:email commit-info) code] (assoc report
                                                  :author-email (:email commit-info)
                                                  :author-name (:author commit-info)
                                                  )])
           (into {})))
    (catch Exception e
      (logging/error e "Error occured during commit-info parsing" (.getName commit)))
    (finally )))


(defn- merge-achievements [a b]
  (cond
    (< (:level a 0) (:level b 0)) a
    (< (:level b 0) (:level a 0)) b
    :else a))

(defn- find-achievements [repo]
 (let [df (git-parser/diff-formatter repo)]
    (->> (for [commit (git-parser/commit-list repo)]
           ;; check that we don't know about this sha1
           (analyze-commit repo commit df))
         (reduce (partial merge-with merge-achievements)))))

(defn- current-achievements [repo-id]
  (->> (db/get-achievements-by-repo repo-id)
       (map (juxt :email :type :level))
       set))

(defn- intersect-achievements [new-achs current-achs]
  (->> new-achs
    (map (fn [[[email code] data]] [[email (name code) (:level data)] data]))
    (remove #(contains? current-achs (first %)))))

(defn- sync-achievements [url new-achs]
  (let [repo-db (db/get-or-insert-repo url)
        current-achs (current-achievements (:id repo-db))]
    (db/with-connection
      (doseq [[[email code level] data] (intersect-achievements new-achs current-achs)]
        (let [user (db/get-or-insert-user email (:author-name data))]
          (db/insert-achievement {:type (name code),
                                  :level level,
                                  :userid (:id user),
                                  :repoid (:id repo-db),
                                  :timestamp (util/format-date (:time data))}))))))

(defn analyze [url]
  (let [repo (git-parser/load-repo url)
        new-achievements (find-achievements repo)]
    (logging/info (time (sync-achievements url new-achievements)))
    
    
    ))

(defn start-next []
  (if-let [repo (db/get-next-repo-to-process)]
    (analyze (:url repo))))