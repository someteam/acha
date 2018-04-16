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

(defn- merge-achievements [a b]
  (cond
    (< (:level a 0) (:level b 0)) a
    (< (:level b 0) (:level a 0)) b
    :else a))

(defn- scan-aches-group [group & args]
  (let [safely-scan (fn [code scanner args]
                      (try
                        (apply scanner args)
                        (catch Exception e
                          (logging/error e "Failed to run achievement scanner" code args))))]
    (->>
      (for [[code scanner] (get achievement/base group)
            report (safely-scan code scanner args)
            :when report
            :let [achievement (-> report
                                (dissoc :commit-info)
                                (merge (select-keys (:commit-info report)
                                                    [:author :calendar :id :email])))
                  akey [(get-in report [:commit-info :email]) code]]]
        [akey achievement])
      (into {}))))

(defn- find-diff-achievements [repo commit-info]
  (let [[files aches] (reduce
                        (fn [[changed-files achievements] changed-file]
                          (let [file-with-diff (git-parser/parse-diff repo changed-file)
                                file-with-loc (git-parser/calculate-loc file-with-diff)
                                new-achievements (scan-aches-group :diff-scanners commit-info file-with-diff)]
                            [(conj changed-files file-with-loc)
                             (merge-with merge-achievements
                               achievements new-achievements)]))
                       nil
                       (:changed-files commit-info))]
    [(assoc commit-info :changed-files files) aches]))

(defn- analyze-commit [repo-info repo commit]
  (try
    (let [commit-info (git-parser/commit-info repo commit)
          [enhanced-commit-info diff-aches] (find-diff-achievements repo commit-info)
          commit-aches (scan-aches-group :commit-scanners enhanced-commit-info)]
      (concat commit-aches diff-aches))
    (catch Exception e
      (logging/error e "Failed commit-info parsing" (.getName commit)))
    (finally
      (db/insert-scanned-commit (:id repo-info) (.getName commit)))))

(defn- find-commit-achievements [repo-info repo]
  (logging/info "Scanning new commits for identity achievements" (:url repo-info))
  (let [seen (db/get-scanned-commits (:id repo-info))]
    (transduce (comp
                 (take 2000)
                 (remove #(contains? seen (.getName %)))
                 (map    #(analyze-commit repo-info repo %)))
               (completing #(merge-with merge-achievements %1 %2))
               {}
               (git-parser/commit-list repo))))

(defn- find-timeline-achievements [repo-info repo]
  (logging/info "Scanning new commits for timeline achievements" (:url repo-info))
  (let [last-snapshot (:snapshot repo-info)
        current-snapshot (->> (git-parser/branches repo) sort hash)]
    (when (not= last-snapshot current-snapshot)
      (try
        (->> (git-parser/commit-list repo)
             (map #(git-parser/commit-info repo %))
             (scan-aches-group :timeline-scanners))
        (finally
          ; todo insert if new or update
          (db/update-scanned-timeline (:id repo-info) current-snapshot))))))

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
          :timestamp (.getTimeInMillis (:calendar data))
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

(defn- worker []
  (logging/info "Worker is ready")
  (loop []
    (try
      (when-let [repo (not-empty (db/get-next-repo-to-process))]
        (try
          (logging/info "Worker has started processing" repo)
          (analyze repo)
          (logging/info "Worker has finished processing" repo)
          (catch Throwable e
            (db/update-repo-state (:id repo) :error (util/reason e))
            (logging/error e "Repo analysis failed"))))
      (Thread/sleep (rand-int 2000))
      (catch InterruptedException e (throw e))
      (catch Exception e
        (logging/error e "Repo selection failed")))
    (recur)))

(defn run-workers []
  (doseq [id (range 4)]
    (doto (Thread. #(worker) (str "worker#" id))
          (.start))))
