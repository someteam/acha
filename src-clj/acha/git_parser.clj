(ns acha.git-parser
  (:require [acha.util :as util]
            [acha.core :as core]
            [clj-jgit.porcelain :as jgit.p]
            [clj-jgit.querying :as jgit.q]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.security MessageDigest]
           [java.io ByteArrayOutputStream]
           [com.jcraft.jsch Session JSch]
           [org.eclipse.jgit.api Git]
           [org.eclipse.jgit.diff ContentSource RawText RawTextComparator DiffFormatter]
           [org.eclipse.jgit.diff DiffAlgorithm DiffAlgorithm$SupportedAlgorithm DiffEntry DiffEntry$Side]
           [org.eclipse.jgit.diff Edit EditList MyersDiff]
           [org.eclipse.jgit.lib ObjectReader ObjectLoader Constants FileMode AbbreviatedObjectId ConfigConstants]
           [org.eclipse.jgit.revwalk RevWalk RevCommit RevTree]
           [org.eclipse.jgit.transport FetchResult JschConfigSessionFactory OpenSshConfig$Host SshSessionFactory]
           [org.eclipse.jgit.treewalk EmptyTreeIterator CanonicalTreeParser AbstractTreeIterator]
           [org.eclipse.jgit.util FS IO]
           [org.eclipse.jgit.util.io DisabledOutputStream]))

(defn- data-dir [url]
  (let [repo-name (->> (string/split url #"/") (remove string/blank?) last)]
    (str core/working-dir "/" repo-name "_" (util/md5 url))))

(def jsch-factory (proxy [JschConfigSessionFactory] []
  (configure [^OpenSshConfig$Host hc ^Session session]
    (.getJSch ^JschConfigSessionFactory this hc FS/DETECTED))))

(SshSessionFactory/setInstance jsch-factory)

(defn- clone [url path]
  (->
    (doto (Git/cloneRepository)
      (.setURI url)
      (.setDirectory (io/as-file path))
      (.setRemote "origin")
      (.setCloneAllBranches true)
      (.setNoCheckout true))
    (.call)))

(defn load-repo ^Git [url]
  (let [path (data-dir url)]
    (if (.exists (io/as-file path))
      (doto (jgit.p/load-repo path)
        (jgit.p/git-fetch-all))
      (clone url path))))

(defn diff-formatter
  [^Git repo]
  (doto (DiffFormatter. DisabledOutputStream/INSTANCE)
    (.setRepository (.getRepository repo))
    (.setDiffComparator RawTextComparator/DEFAULT)
    (.setDetectRenames true)))

(defn object-reader [^Git repo]
  (.. repo getRepository newObjectReader))

(defn- normalize-path [path]
  (cond
    (= path "/") "/"
    (= (first path) \/) (subs path 1)
    :else path))

(defn- change-kind
  [^DiffEntry entry]
  (let [change (.. entry getChangeType name)]
    (cond
      (= change "ADD") :add
      (= change "MODIFY") :edit
      (= change "DELETE") :delete
      (= change "COPY") :copy
      (= change "RENAME") :rename)))


(def commit-list jgit.q/rev-list)

(defn- complete-id [^DiffEntry entry side ^ObjectReader reader]
  (let [id (.getId entry side)]
    (if (.isComplete id)
      id
      (when-first [obj-id (.resolve reader id)]
        (AbbreviatedObjectId/fromObjectId obj-id)))))

(def binary-file-threshold (* 2 1024))
(def big-file-threshold (* 10 1024 1024))
(def empty-array (byte-array 0))

(def raw-comparator RawTextComparator/DEFAULT)
(def diff-algorithm (DiffAlgorithm/getAlgorithm DiffAlgorithm$SupportedAlgorithm/HISTOGRAM))

(defn- parse-edit-list [^EditList el ^RawText a ^RawText b]
  (->> el
    (mapv (fn [^Edit e]
          {:removed (mapv #(vector (.getString a %) %) (range (.getBeginA e) (.getEndA e)))
           :added   (mapv #(vector (.getString b %) %) (range (.getBeginB e) (.getEndB e)))}))))

(defn- parse-change-kind
  [^DiffEntry entry]
  (let [change-kind (change-kind entry)
        old-file {:id (-> entry .getOldId .name)}
        new-file {:id (-> entry .getNewId .name), :path (normalize-path (.getNewPath entry))}]
    (case change-kind
      :add    {:kind change-kind, :new-file new-file}
      :delete {:kind change-kind, :old-file old-file}
      {:kind change-kind, :old-file old-file, :new-file new-file})))

(defn- calc-loc [diff]
  (reduce (fn [s {:keys [added removed]}]
            (-> s
              (update-in [:added] + (count added))
              (update-in [:removed] + (count removed))))
    {:added 0 :removed 0}
    diff))

(defn- load-obj [^AbbreviatedObjectId id ^String path ^ObjectReader reader]
  (let [loader (.. (ContentSource/create reader)
                   (open path (.toObjectId id)))
        stream (.openStream loader)
        size (.getSize stream)
        pre-bf (byte-array (min size binary-file-threshold))]
    (try
      (IO/readFully stream pre-bf 0)
      (if (RawText/isBinary pre-bf)
        [{:type :binary, :size size, :path path} empty-array]
        (let [full-bf (byte-array (min size big-file-threshold))]
          (System/arraycopy pre-bf 0 full-bf 0 (alength pre-bf))
          (IO/readFully stream full-bf (alength pre-bf) (- (alength full-bf) (alength pre-bf)))
          [{:type :text, :size size, :path path} full-bf]))
      (finally 
        (.close stream)))))

(defn- open-diff [^DiffEntry entry side ^ObjectReader reader]
  (let [mode (.getMode entry side)]
    (when (and (not= FileMode/MISSING mode)
               (= Constants/OBJ_BLOB (.getObjectType mode)))
      (let [id (complete-id entry side reader)
            path (.getPath entry side)]
        (load-obj id path reader)))))

(defn- parse-diff-changes [^DiffEntry entry ^DiffAlgorithm alg ^ObjectReader reader]
  (when-not (or (= FileMode/GITLINK (.getOldMode entry))
                (= FileMode/GITLINK (.getNewMode entry))
                (nil? (.getOldId entry))
                (nil? (.getNewId entry)))
    (let [[new-file new-bytes] (open-diff entry DiffEntry$Side/NEW reader)
          [old-file old-bytes] (open-diff entry DiffEntry$Side/OLD reader)
           new-raw (RawText. (or new-bytes empty-array))
           old-raw (RawText. (or old-bytes empty-array))]
      (cond-> {:old-file old-file, :new-file new-file}
        (and (not= :binary (:type new-file))
             (not= :binary (:type old-file)))
        (assoc :diff (-> (.diff alg raw-comparator old-raw new-raw)
                         (parse-edit-list old-raw new-raw)))))))

(defn- parse-diff-entry [^ObjectReader reader ^DiffEntry entry]
  (let [{:keys [kind old-file new-file]} (parse-change-kind entry)
        {:keys [diff] :as diff-changes} (parse-diff-changes entry diff-algorithm reader)
        has-diffs? (not (empty? diff))]
    (cond-> {:kind kind,
              :old-file (merge old-file (:old-file diff-changes))
              :new-file (merge new-file (:new-file diff-changes))}
      has-diffs? (assoc :diff diff)
      has-diffs? (assoc :loc (calc-loc diff)))))

(defn- tree-iterator ^AbstractTreeIterator [^RevCommit commit ^ObjectReader reader]
  (if commit
    (doto (CanonicalTreeParser.) (.reset reader (.getTree commit)))
    (EmptyTreeIterator.)))

(defn- commit-info-core [^Git repo ^RevCommit rev-commit ^DiffFormatter df ^ObjectReader reader diff-parser]
  (let [parent-tree (tree-iterator (first (.getParents rev-commit)) reader)
        commit-tree (tree-iterator rev-commit reader)
        diffs (.scan df parent-tree commit-tree)
        ident (.getAuthorIdent rev-commit)
        time  (.getWhen ident)
        timezone (.getTimeZone ident)
        message (-> (.getFullMessage rev-commit) str string/trim)]
    {:id (.getName rev-commit)
     :author (.getName ident)
     :email  (util/normalize-str (.getEmailAddress ident))
     :calendar (util/create-calendar time timezone)
     :between-time (- (.getCommitTime rev-commit) (.getTime (.getWhen ident)))
     :message message
     :parents (mapv #(.getName %) (.getParents rev-commit))
     :changed-files (mapv diff-parser diffs)}))

(defn commit-info-without-diffs [^Git repo ^RevCommit rev-commit ^DiffFormatter df ^ObjectReader reader]
  (commit-info-core repo rev-commit df reader parse-change-kind))

(defn commit-info [^Git repo ^RevCommit rev-commit ^DiffFormatter df ^ObjectReader reader]
  (commit-info-core repo rev-commit df reader (partial parse-diff-entry reader)))

(defn branches [^Git repo]
  (->> (clj-jgit.porcelain/git-branch-list repo)
       (map #(.. % (getObjectId) (getName)))))

(defn- repo-info [url]
  (let [repo (load-repo url)
        reader (-> repo .getRepository .newObjectReader)
        formatter (diff-formatter repo)]
    (map #(commit-info repo % formatter reader) (jgit.q/rev-list repo))))
