(ns acha.git-parser
  (:require [acha.util :as util]
            [clj-jgit.porcelain :as jgit.p]
            [clj-jgit.querying :as jgit.q]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.security MessageDigest]
           [java.io ByteArrayOutputStream]
           [org.eclipse.jgit.diff DiffFormatter DiffEntry]
           [org.eclipse.jgit.diff RawTextComparator]
           [org.eclipse.jgit.lib ObjectReader]
           [org.eclipse.jgit.api Git]
           [org.eclipse.jgit.revwalk RevWalk RevCommit RevTree]
           [org.eclipse.jgit.treewalk EmptyTreeIterator CanonicalTreeParser AbstractTreeIterator]))

(defn- data-dir [url]
  (let [repo-name (last (string/split url #"/"))]
    (str "./remotes/" repo-name "_" (util/md5 url))))

(defn load-repo [url]
  (let [path (str (data-dir url) "/repo")]
    (if (.exists (io/as-file path))
      ;; todo fetch and hard reset too
      (jgit.p/load-repo path)
      (jgit.p/git-clone url path))))

(gen-interface
  :name achievement.git.IDiffStatsProvider
  :methods [[calculateDiffs [java.util.List] clojure.lang.APersistentMap]
            [treeIterator [org.eclipse.jgit.revwalk.RevCommit] org.eclipse.jgit.treewalk.AbstractTreeIterator ]])

(defn diff-formatter
  [^Git repo]
  (let [stats (atom {})
        stream (ByteArrayOutputStream.)
        reader (-> repo .getRepository .newObjectReader)
        diffs (atom [])
        section (atom [])
        formatter (proxy [DiffFormatter achievement.git.IDiffStatsProvider] [stream]
                    (writeAddedLine [text line]
                      (swap! section conj [:add (.getString text line) line])
                      (swap! stats update-in [:loc :added] (fnil inc 0)))
                    (writeRemovedLine [text line]
                      (swap! section conj [:remove (.getString text line) line])
                      (swap! stats update-in [:loc :deleted] (fnil inc 0)))
                    (writeHunkHeader [& _]
                      (swap! diffs conj @section)
                      (reset! section []))
                    (treeIterator [commit]
                      (if commit
                        (doto (CanonicalTreeParser.) (.reset reader (.getTree commit)))
                        (EmptyTreeIterator.)))
                    (calculateDiffs [diff-entities]
                      (reset! diffs [])
                      (reset! section [])
                      (reset! stats nil)
                      (.reset stream)
                      (proxy-super format diff-entities)
                      (proxy-super flush)
                      (when-not (empty? @section)
                        (swap! diffs conj @section))
                      (assoc @stats :diffs @diffs)))]
    (doto formatter
      (.setRepository (.getRepository repo))
      (.setDiffComparator RawTextComparator/DEFAULT))))

(defn- normalize-path
  [path]
  (if (= path "/")
    "/"
    (if (= (first path) \/)
      (apply str (rest path))
      path)))

(defn- change-kind
  [^DiffEntry entry]
  (let [change (.. entry getChangeType name)]
    (cond
      (= change "ADD") :add
      (= change "MODIFY") :edit
      (= change "DELETE") :delete
      (= change "COPY") :copy)))

(defn- parse-diff-entry
  [^DiffEntry entry]
  (let [change-kind (change-kind entry)
        old-file {:id (-> entry .getOldId .name)};, :path (normalize-path (.getOldPath entry))}
        new-file {:id (-> entry .getNewId .name), :path (normalize-path (.getNewPath entry))}]
    (case change-kind
      :edit   [change-kind old-file new-file]
      :add    [change-kind nil new-file]
      :delete [change-kind old-file nil]
      :else   [change-kind old-file new-file])))

(defn commit-info [^Git repo ^RevCommit rev-commit ^DiffFormatter df]
  (let [parent-tree (.treeIterator df (first (.getParents rev-commit)))
        commit-tree (.treeIterator df rev-commit)
        diffs (.scan df parent-tree commit-tree)
        ident (.getAuthorIdent rev-commit)
        time (-> (.getCommitTime rev-commit) (* 1000) java.util.Date.)
        message (-> (.getFullMessage rev-commit) str string/trim)]
    (merge {:id (.getName rev-commit)
            :author (.getName ident)
            :email (.getEmailAddress ident)
            :time time
            :message message
            :changed-files (mapv parse-diff-entry diffs)
            :merge (> (.getParentCount rev-commit) 1)}
           (.calculateDiffs df diffs))))

(def commit-list jgit.q/rev-list)

(defn- repo-info [url]
  (let [repo (load-repo url)
        formatter (diff-formatter repo)]
    (map #(commit-info repo % formatter) (jgit.q/rev-list repo))))

(defn setup []
 (repo-info "https://github.com/tonsky/datascript.git"))
;  (spit "test_rails" (prn-str (repo-info "https://github.com/rails/rails.git"))))

