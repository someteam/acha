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

(defn- analyze-commit [repo-info repo commit df]
  (try
;;     (logging/info "Analyzing" (:url repo-info) (.getName commit) (.. commit getAuthorIdent getWhen))
    (let [commit-info (git-parser/commit-info repo commit df)]
      (when (not (:merge commit-info))
        (->> (for [[code scanner] achievement/all-commit-info-scanners
                 :let [report (scan-achievement scanner commit-info)]
                 :when report]
             [[(:email commit-info) code] (-> report
                                            (assoc-in [:author :email] (:email commit-info))
                                            (assoc-in [:author :name]  (:author commit-info))
                                            (assoc-in [:sha1] (:id commit-info)))])
           (into {}))))
    (catch Exception e
      (logging/error e "Error occured during commit-info parsing" (.getName commit)))
    (finally
      (db/insert-repo-seen-commit (:id repo-info) (.getName commit)))))


(defn- merge-achievements [a b]
  (cond
    (< (:level a 0) (:level b 0)) a
    (< (:level b 0) (:level a 0)) b
    :else a))

(defn- find-achievements [repo-info repo]
  (logging/info "Scanning new commits for achievements" (:url repo-info))
  (let [df      (git-parser/diff-formatter repo)
        seen    (db/get-repo-seen-commits (:id repo-info))]
    (->> (git-parser/commit-list repo)
         (take 2000)
         (remove #(contains? seen (.getName %)))
         (map    #(analyze-commit repo-info repo % df))
         (reduce #(merge-with merge-achievements %1 %2) {}))))

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
    (doseq [[[email code level] data] (intersect-achievements new-achs current-achs)]
      (let [user (db/get-or-insert-user email (get-in data [:author :name]))]
        (db/insert-achievement {:type (name code)
                :level level
                :userid (:id user)
                :repoid (:id repo-info)
                :sha1 (:sha1 data)
                :timestamp (util/format-date (:time data))})))))

(defn analyze [repo-info]
  (logging/info "Fetching/cloning repo" (:url repo-info))
  (let [repo (git-parser/load-repo (:url repo-info))
        new-achievements (find-achievements repo-info repo)]
    (logging/info "Add new achievements to db for" (:url repo-info))
    (sync-achievements repo-info new-achievements)
    (db/update-repo-state (:id repo-info) "ok")))

(defn- worker [worker-id]
  (logging/info "Worker #" worker-id " is ready")
  (loop []
    (try
      (when-let [repo (not-empty (db/get-next-repo-to-process))]
        (logging/info "Worker #" worker-id "has started processing" repo)
        (analyze repo)
        (logging/info "Worker #" worker-id "has finished processing" repo))
      (Thread/sleep (rand-int 2000))
      (catch InterruptedException e (throw e))
      (catch Exception e
        (logging/error e "Catch exception during repo analysing")))
    (recur)))

(defn run-workers []
  (doseq [id (range 1)]
    (future (worker id))))
