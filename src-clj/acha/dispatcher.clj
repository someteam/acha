(ns acha.dispatcher
  (:require
    [clojure.tools.logging :as logging]
    [clojure.java.jdbc :as jdbc]
    [clojure.core.async :as async]
    [acha.achievement :as achievement]
    [acha.db :as db]
    [acha.util :as util]
    [acha.core]
    [acha.git-parser :as git-parser]))

(defn- scan-achievement [scanner obj]
  (try
    (scanner obj)
    (catch Exception e
      (logging/error e "Failed commit achievement scan"))))

(defn- analyze-commit [repo-info repo commit df reader]
  (try
    (let [commit-info (git-parser/commit-info repo commit df reader)]
      (->> (for [[code scanner] (:commit-scanners achievement/base)
                 :let [report (scan-achievement scanner commit-info)]
                 :when report]
           [[(:email commit-info) code] (merge report
                                               (select-keys commit-info [:author :time :id]))])
         (into {})))
    (catch Exception e
      (logging/error e "Failed commit-info parsing" (.getName commit)))
    (finally
      (db/insert-repo-seen-commit (:id repo-info) (.getName commit)))))

(defn- merge-achievements [a b]
  (cond
    (< (:level a 0) (:level b 0)) a
    (< (:level b 0) (:level a 0)) b
    :else a))

(defn- find-commit-achievements [repo-info repo]
  (logging/info "Scanning new commits for identity achievements" (:url repo-info))
  (let [seen (db/get-repo-seen-commits (:id repo-info))
        df (git-parser/diff-formatter repo)
        reader (git-parser/object-reader repo)]
    (try
      (transduce (comp
                   (take 2000)
                   (remove #(contains? seen (.getName %)))
                   (map    #(analyze-commit repo-info repo % df reader)))
                 (completing #(merge-with merge-achievements %1 %2))
                 {}
                 (git-parser/commit-list repo))
      (finally
        (.release reader)
        (.release df)))))

(defn- find-timeline-achievements [repo-info repo]
  (logging/info "Scanning new commits for timeline achievements" (:url repo-info))
  (let [df (git-parser/diff-formatter repo)
        reader (git-parser/object-reader repo)]
    (try
      (let [commits (mapv #(git-parser/commit-info-without-diffs repo % df reader)
                           (git-parser/commit-list repo))]
        (->> (for [[code scanner] (:timeline-scanners achievement/base)
                   report (scan-achievement scanner commits)
                   :when report
                   :let [commit-info (:commit-info report)]]
             [[(:email commit-info) code] (merge report
                                                 (select-keys commit-info [:author :time :id]))])
           (into {})))
      (finally
        (.release reader)
        (.release df)))))

(defn- find-achievements [repo-info repo]
  (merge-with merge-achievements
    (find-commit-achievements repo-info repo)
    (find-timeline-achievements repo-info repo)))

(defn- sync-achievements [repo-info new-achs]
  (let [current-achs  (into #{}
                            (map (juxt :email :type :level))
                            (db/get-achievements-by-repo (:id repo-info)))
        new-achs      (->> new-achs
                           (remove
                             (fn [[[email code] data]]
                               (contains? current-achs [email (name code) (:level data)]))))]
    (db/insert-users
      (->> new-achs
        (map (fn [[[email code] data]]
               {:email email :name (:author data)}))))
    (db/insert-achievements
      (for [[[email code] data] new-achs]
        { :type      (name code)
          :level     (:level data)
          :userid    (:id (db/get-user-by-email email))
          :repoid    (:id repo-info)
          :sha1      (:id data)
          :timestamp (.getTime (:time data))
          :assigned  (System/currentTimeMillis) }))))

(defn analyze [repo-info]
  (let [{:keys [id url]} repo-info]
    (db/update-repo-state id :fetching)
    (logging/info "Fetching/cloning repo" url)
    (let [repo (git-parser/load-repo url)
          _    (db/update-repo-state id :scanning)
          new-achievements (find-achievements repo-info repo)]
      (logging/info "Add new achievements to db for" url)
      (db/update-repo-state id :storing)
      (sync-achievements repo-info new-achievements)
      (db/update-repo-state id :idle))))

(defn- worker [worker-id]
  (logging/info "Worker #" worker-id " is ready")
  (loop []
    (try
      (when-let [repo (not-empty (db/get-next-repo-to-process))]
        (try
          (logging/info "Worker #" worker-id "has started processing" repo)
          (analyze repo)
          (logging/info "Worker #" worker-id "has finished processing" repo)
          (catch Exception e
            (db/update-repo-state (:id repo) :error (util/reason e))
            (logging/error e "Repo analysis failed"))))
      (Thread/sleep (rand-int 2000))
      (catch InterruptedException e (throw e))
      (catch Exception e
        (logging/error e "Repo selection failed")))
    (recur)))

(defn run-workers []
  (doseq [id (range 4)]
    (future (worker id))))
