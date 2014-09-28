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
             [[(:email commit-info) code] (-> report
                                            (assoc-in [:author :email] (:email commit-info))
                                            (assoc-in [:author :name]  (:author commit-info))
                                            (assoc-in [:sha1] (:id commit-info)))])
           (into {})))
    (catch Exception e
      (logging/error e "Error occured during commit-info parsing" (.getName commit)))
    (finally )))


(defn- merge-achievements [a b]
  (cond
    (< (:level a 0) (:level b 0)) a
    (< (:level b 0) (:level a 0)) b
    :else a))

(defn- find-achievements [repo-info repo]
 (let [df (git-parser/diff-formatter repo)]
    (->> (for [commit (git-parser/commit-list repo)
               :while (or (nil? (:sha1 repo-info))
                          (not= (.getName commit) (:sha1 repo-info)))]
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

(defn- sync-achievements [repo-info new-achs]
  (let [current-achs (current-achievements (:id repo-info))]
    (db/with-connection
      (doseq [[[email code level] data] (intersect-achievements new-achs current-achs)]
        (let [user (db/get-or-insert-user email (get-in data [:author :name]))]
          (db/insert-achievement {:type (name code)
                                  :level level
                                  :userid (:id user)
                                  :repoid (:id repo-info)
                                  :sha1 (:sha1 data)
                                  :timestamp (util/format-date (:time data))}))))))

(defn- sync-repo-sha1 [repo-info repo]
  (let [sha1 (git-parser/head-sha1 repo)]
    (db/update-repo-sha1 (:id repo-info) sha1)))

(defn analyze [repo-info]
  (let [repo (git-parser/load-repo (:url repo-info))
        new-achievements (find-achievements repo-info repo)]
    (sync-achievements repo-info new-achievements)
    (sync-repo-sha1 repo-info repo)))

(defn- worker [worker-id]
  (logging/info "Worker #" worker-id " is ready")
  (loop []
    (try
      (when-let [repo (db/get-next-repo-to-process)]
        (logging/info "Worker #" worker-id " has started processing" (:url repo))
        (analyze repo))
      (Thread/sleep 1000)
      (catch InterruptedException e (throw e))
      (catch Exception e
        (logging/error e "Catch exception during repo analysing")))))

(defn run-workers []
  (doseq [id (range 4)]
    (future (worker id))))
