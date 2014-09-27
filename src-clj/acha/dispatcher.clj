(ns acha.dispatcher
  (:require
    [clojure.tools.logging :as logging]
    [acha.achievement :as achievement]
    [acha.git-parser :as git-parser]))

(defn- apply-achievement [scanner commit-info]
  (try
    (when-let [result (scanner commit-info)]

      ;; store it to database
     ;; (logging/info result)
      
      )

    (catch Exception e
      (logging/error e "Error occured during achivement scan commit"))))

(defn- analyze-commit [repo commit df]
  (try
    (let [commit-info (git-parser/commit-info repo commit df)]
      (doseq [scanner achievement/all-commit-info-scanners]
        (apply-achievement scanner commit-info)))
    (catch Exception e
      (logging/error e "Error occured during commit-info parsing" (.getName commit)))
    (finally )))


(defn analyze [url]
  (let [repo (git-parser/load-repo url)
        df (git-parser/diff-formatter repo)]
    (doseq [commit (git-parser/commit-list repo)]
      (analyze-commit repo commit df))))


