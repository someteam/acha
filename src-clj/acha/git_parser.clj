(ns acha.git-parser
  (:require [acha.util :as util]
            [clj-jgit.porcelain :as jgit.p]
            [clojure.java.io :as io]
            [clojure.string :as string]))


(defn- data-dir [url]
  (let [repo-name (last (string/split url #"/"))]
    (str "./remotes/" repo-name "_" (util/md5 url))))

(defn- load-repo [url]
  (let [path (str (data-dir url) "/repo")]
    (if (.exists (io/as-file path))
      (jgit.p/load-repo path)
      (jgit.p/git-clone url path))))

(defn setup []
  (load-repo "https://github.com/tonsky/datascript.git"))
