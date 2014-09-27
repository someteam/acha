(ns acha.dispatcher
  (:require
    [clojure.tools.logging :as logging]
    [acha.git-parser :as git-parser]))

(defn analyze [url]
  (let [repo (git-parser/load-repo url)
        df (git-parser/diff-formatter repo)]
    (doseq [commit (git-parser/commit-list repo)]
      (try
        (let [commit-info (git-parser/commit-info repo commit df)]
          (logging/info commit-info))
        (catch Exception e
          (logging/error e "Unable to parse commit"))
        (finally 
          ;; mark this tweets as viewed)))))
