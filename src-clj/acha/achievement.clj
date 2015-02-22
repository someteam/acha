(ns acha.achievement
  (:use clojure.data)
  (:require
    [clojure.set :as set]
    [clojure.tools.logging :as logging]
    [acha.util :as util]
    [acha.achievement-static]
    [acha.spellings :as spellings]
    [acha.swears :as swears]
    [acha.emoji :as emoji]
    [clojure.string :as string])
  (:import
    [java.util Calendar]))

(defmacro defscanners
  [name & scanners]
  `(def ~name (-> {} ~@scanners)))

(defn commit-scanner
  [scanners sname f & {:keys [include-merges]}]
  (update-in scanners [:commit-scanners] assoc sname
    (fn [commit-info]
      (when (or include-merges (<= (count (:parents commit-info)) 1))
        (when-let [ret (f commit-info)]
          (if (map? ret) ret {}))))))

(defn timeline-scanner
  [scanners sname f]
  (update-in scanners [:timeline-scanners] assoc sname f))

(defn- scan-word-count [text words]
  (->> (string/split text #"\b") (filter words) count))

(defn- calculate-loc
  ([changed-files] (calculate-loc changed-files identity))
  ([changed-files xf]
    (transduce
      (comp (map :loc) xf)
      (completing #(merge-with + %1 %2))
      {:added 0 :removed 0}
      changed-files)))

(defn- date-scanner [scanners sname month day]
  (commit-scanner scanners sname
    (fn [{:keys [calendar]}]
      (and (= month (+ 1 (.get calendar Calendar/MONTH)))
           (= day (.get calendar Calendar/DAY_OF_MONTH))))))

(defn- subword-scanner [scanners sname words]
  (commit-scanner scanners sname
    (fn [commit-info]
      (pos? (scan-word-count (:message commit-info) words)))))

(defn- scan-filenames [rexps changed-files kinds]
  (not-empty
    (for [file (filter #(kinds (:kind %)) changed-files)
          :let [path (get-in file [:new-file :path])]
          :when (some #(re-matches % path) rexps)]
      file)))

(defn- language-scanner [scanners sname exts & {:keys [multi-listed?]
                                                :or {multi-listed? true}}]
  (let [rexps (map #(re-pattern (str "(?i)(?:.+)\\." % "$")) exts)]
    (cond-> scanners
      multi-listed? (update-in [:languages] assoc sname rexps)
      true (commit-scanner sname
             (fn [{:keys [changed-files]}]
               (scan-filenames rexps changed-files #{:add}))))))

(defn- check-changed-lines [f changed-files]
  (some->> (for [{:keys [diff]} changed-files
                 {:keys [added removed]} diff]
             (when (and (not= 0 (count added))
                        (= (count added) (count removed)))
               (every? (fn [[[l1 _] [l2 _]]] (f l1 l2))
                       (map vector added removed))))
           not-empty
           (every? identity)))

(defn- parentage [commits]
  (->> (for [child commits, parent (:parents child)]
         [parent child])
       (reduce (fn [acc [k v]] (update-in acc [k] conj v)) {})))

(defn calculate-combos
  [commits]
  (let [sha->commit (util/map-by :id commits)
        ;; determine relationship between parent and children
        parent->children (parentage commits)
        init-commits (filter #(= 0 (count (:parents %))) commits)]
    (loop [[current & rest-queue] (seq init-commits)
           combos {}]
      (if current
        ;; determine current combo streak from commit parents
        (let [length (->> (:parents current)
                          (map sha->commit)
                          (filter #(= (:email %) (:email current)))
                          (keep combos)
                          (reduce max 0) inc)]
          ;; add children to queue if we could increase their combo streak
          (recur (concat
                   (->> (get parent->children (:id current))
                        (filter (fn [child]
                                  (or (not (contains? combos child)) ; unvisited node
                                      (and (= (:email child) (:email current)) ; it's a combo and I can prolong it
                                           (< (combos child) (inc length)))))))
                   rest-queue)
                 (update-in combos [current] (fnil max 0) length)))
        combos))))

(defscanners base
  (commit-scanner :bad-motherfucker
    (fn [{:keys [message]}]
      (let [word-count (scan-word-count message swears/table)]
        (when (pos? word-count) {:level word-count}))))

  (commit-scanner :hello-linus
    (fn [commit-info]
      (when-let [a ((get-in base [:commit-scanners :bad-motherfucker]) commit-info)]
        (<= 5 (:level a)))))

  (commit-scanner :borat
    (fn [{:keys [message]}]
      (let [word-count (scan-word-count message spellings/table)]
        (when (pos? word-count) {:level word-count}))))

  (commit-scanner :leo-tolstoy
    (fn [{:keys [message]}]
      (<= 10 (-> message string/split-lines count))))

  (commit-scanner :man-of-few-words
    (fn [{:keys [message]}]
      (< (count message) 4)))

  (commit-scanner :no-more-letters
    (fn [{:keys [^String message]}]
      (not (some #(Character/isLetter (.charValue %)) message))))

  (commit-scanner :narcissist
    (fn [{:keys [^String message author]}]
      (.contains (util/normalize-str message)
                 (util/normalize-str author))))

  ;; SHA achievements
  (commit-scanner :lucky #(.contains ^String (:id %) "777"))

  (commit-scanner :mark-of-the-beast #(.contains ^String (:id %) "666"))

  ;; LOC achievements
  (commit-scanner :world-balance
    (fn [{:keys [changed-files]}]
      (let [loc (calculate-loc changed-files (filter #(not= (:added %) (:removed %))))]
         (and (pos? (:added loc))
              (= (:added loc) (:removed loc))))))

  (commit-scanner :eraser
    (fn [{:keys [changed-files]}]
      (let [loc (calculate-loc changed-files)]
        (and (= 0 (:added loc)) (pos? (:removed loc))))))

  (commit-scanner :massive
    (fn [{:keys [changed-files]}]
      (let [loc (calculate-loc changed-files)]
        (<= 1000 (:added loc)))))

  ;; DIFF achievements
  (commit-scanner :easy-fix
    (fn [{:keys [changed-files]}]
      (and
        ; only one file was changed
        (= (count changed-files) 1)
        (let [diff (get-in changed-files [0 :diff])]
          (or
            ; swap two nonadjacent lines
            (and
              (= 2 (count diff))
              ; one not blank lines
              (->> (for [d [0 1], m [:added :removed]]
                     (and (= 1 (count (get-in diff [d m])))
                          (not (string/blank? (get-in diff [d m 0 0])))))
                   (every? true?))
              ; content added in one edit == removed in another edit
              (= (get-in diff [0 :added 0 0])   (get-in diff [1 :removed 0 0]))
              (= (get-in diff [1 :added 0 0])   (get-in diff [0 :removed 0 0])))

            ; swap two adjacent lines
            (and
              (= 1 (count diff))
              ; one line
              (= 1 (count (get-in diff [0 :added])))
              (= 1 (count (get-in diff [0 :removed])))
              ; not blank line
              (not (string/blank? (get-in diff [0 :added 0 0])))
              ; the smae
              (= (get-in diff [0 :added 0 0])   (get-in diff [0 :removed 0 0]))
              ; adjacent
              (= 1 (Math/abs (- (get-in diff [0 :added 0 1])
                                (get-in diff [0 :removed 0 1]))))))))))

  ;; COMMIT DATE AND TIME
  (commit-scanner :programmers-day
    (fn [{:keys [calendar]}]
      (= 256 (.get calendar Calendar/DAY_OF_YEAR))))

  (commit-scanner :thanksgiving
    (fn [{:keys [calendar]}]
       (and (= 10 (.get calendar Calendar/MONTH)) ;; November
            (= 5 (.get calendar Calendar/DAY_OF_WEEK)) ;; Thursday
            (<= 22 (.get calendar Calendar/DAY_OF_MONTH) 28)))) ;; 4th Thursday

  (commit-scanner :owl
    (fn [{:keys [calendar]}]
      (<= 4 (.get calendar Calendar/HOUR_OF_DAY) 7))) ;; between 4am and 7am

  (commit-scanner :dangerous-game
    (fn [{:keys [calendar]}]
      (and (<= 18 (.get calendar Calendar/HOUR_OF_DAY)) ;; after 18PM
           (= 6 (.get calendar Calendar/DAY_OF_WEEK))))) ;; Friday

  (commit-scanner :time-get
    (fn [{:keys [calendar]}]
      (and (= 0 (.get calendar Calendar/HOUR_OF_DAY))
           (= 0 (.get calendar Calendar/MINUTE)))))

  (commit-scanner :alzheimers
    (fn [{:keys [between-time]}]
      (<= (* 60 60 24 30) between-time)))

  (commit-scanner :mover
    (fn [{:keys [changed-files]}]
      (some #(and (= (:kind %) :rename)
                  (= 0 (count (:diff %)))) changed-files)))

  ;; ASK. It's not possible to have a commit without message or without files (except merges)
  (commit-scanner :empty-commit
    (fn [{:keys [changed-files]}]
      (= (count changed-files) 0)))

  (commit-scanner :wrecking-ball
    (fn [{:keys [changed-files]}]
      (<= 100 (count changed-files))))

  (commit-scanner :emoji
    (fn [{:keys [message]}]
      (emoji/contains-emoji? message)))

  (commit-scanner :fat-ass
    (fn [{:keys [changed-files]}]
      (some #(and (= :add (:kind %))
                  (<= (* 2 1024 1024) (get-in % [:new-file :size] 0)))
            changed-files)))

  (commit-scanner :holy-war
    (fn [{:keys [changed-files]}]
      (let [war? (fn [^String line-a ^String line-b]
                   (and ;; one line has tab another doesn't have tab
                        (or (and (re-find #"\t(.*?)\S" line-a)
                                 (not (re-find #"\t(.*?)\S" line-b)))
                            (and (re-find #"\t(.*?)\S" line-b)
                                 (not (re-find #"\t(.*?)\S" line-a))))
                        ;; not whitespace characters are the same
                        (= (clojure.string/replace line-a #"\s" "")
                           (clojure.string/replace line-b #"\s" ""))
                        ;; number of space characters is changed (not tab deletion)
                        (not= (clojure.string/replace line-a #"[^ ]" "")
                              (clojure.string/replace line-b #"[^ ]" ""))))]
        (check-changed-lines war? changed-files))))

  (commit-scanner :ocd
    (fn [{:keys [changed-files]}]
      (let [ocd? (fn [^String line-a ^String line-b]
                   (and 
                     ;; line-b is a part of line-a
                     (.startsWith line-a line-b)
                     ;; the rest part contains spaces
                     (re-matches #"^[ \t]+$" (.substring line-a (.length line-b)))))]
        (check-changed-lines #(ocd? %2 %1) changed-files))))

  (date-scanner :christmas 12 25)
  (date-scanner :halloween 10 31)
  (date-scanner :fools-day 4 1)
  (date-scanner :leap-day 2 29)
  (date-scanner :new-year 1 1)
  (date-scanner :russia-day 6 12)
  (date-scanner :valentine 2 14)

  (commit-scanner :citation-needed
    (fn [{:keys [message]}]
      (re-find #"(?i)(?:http(?:s)?:\/\/)?(?:www\.)?stackoverflow\.com" message)))

  (subword-scanner :beggar #{"achievement" "achievements"})
  (subword-scanner :fix #{"fix" "fixes" "fixed" "fixing"})
  (subword-scanner :forgot #{"forgot"})
  (subword-scanner :google #{"google"})
  (subword-scanner :hack #{"hack"})
  (subword-scanner :impossible #{"impossible"})
  (subword-scanner :magic #{"magic"})
  (subword-scanner :never-probably #{"later"})
  (subword-scanner :secure #{"secure"})
  (subword-scanner :wow #{"wow"})

  (language-scanner :basic ["bas" "vb" "vbs" "vba" "vbproj"])
  (language-scanner :c-sharp ["cs" "csproj"])
  (language-scanner :clojure ["clj" "cljx"])
  (language-scanner :clojurescript ["cljs"])
  (language-scanner :css ["css" "sass" "scss" "less" "haml"] :multi-listed? false)
  (language-scanner :cxx ["c++" "cc" "cpp" "cxx" "pcc" "hh" "hpp" "hxx" "vcproj"])
  (language-scanner :dart ["dart"])
  (language-scanner :emacs-lisp ["el"])
  (language-scanner :erlang ["erl" "hrl"])
  (language-scanner :go ["go"])
  (language-scanner :haskell ["hs" "lhs"])
  (language-scanner :java ["java" "jsf" "jsp" "jspf"])
  (language-scanner :javascript ["js"])
  (language-scanner :objective-c ["m" "mm"])
  (language-scanner :pascal ["pas"])
  (language-scanner :perl ["pl"])
  (language-scanner :php ["php" "php3" "php4" "php5"])
  (language-scanner :python ["py"])
  (language-scanner :ruby ["rake" "rb"])
  (language-scanner :scala ["scala"])
  (language-scanner :shell ["bash" "sh" "awk" "sed"])
  (language-scanner :sql ["sql"] :multi-listed? false)
  (language-scanner :swift ["swift"])
  (language-scanner :windows-language ["bat" "btm" "cmd" "ps1" "xaml"])
  (language-scanner :xml ["xml" "xsl" "xslt" "xsd" "dtd"] :multi-listed? false)

  (commit-scanner :nothing-to-hide
    (fn [{:keys [changed-files]}]
      (scan-filenames [#"(?i)(?:.*)id_rsa$"] changed-files #{:add})))

  (commit-scanner :scribbler
    (fn [{:keys [changed-files]}]
      (scan-filenames [#"(?i)^readme(\.md)?$"] changed-files #{:add})))

  (commit-scanner :change-of-mind
    (fn [{:keys [changed-files]}]
      (scan-filenames [#"(?i)^license(\.md)?$"] changed-files #{:edit})))

  (commit-scanner :multilingual
    (fn [{:keys [changed-files]}]
      (let [threshold 3
            level (->>
                     (get-in base [:languages])
                     (filter #(scan-filenames (second %) changed-files #{:add :edit}))
                     count)]
        (when (<= threshold level)
          {:level (inc (- level threshold))}))))

  (commit-scanner :for-stallman
    (fn [{:keys [changed-files]}]
      (when-let [licenses (scan-filenames [#"(?i)^license(\.md)?$"] changed-files #{:add})]
        (some identity
          (for [{:keys [diff]} licenses
                {:keys [added _]} diff
                [line _] (take 10 added)] ; first 10 lines
            (re-find #"(?i)\bgnu\b(.*?)\blicense\b" line))))))

  (commit-scanner :hydra
    (fn [{:keys [parents]}]
      (<= 3 (count parents)))
    :include-merges true)

  (timeline-scanner :anniversary
    (fn [commits]
      (when-first [init-commit (filter #(= 0 (count (:parents %))) commits)]
        (let [birthday (:calendar init-commit)
              anniv-commits (filter (fn [{:keys [calendar]}]
                                      (and (= (.get birthday Calendar/MONTH) (.get calendar Calendar/MONTH))
                                           (= (.get birthday Calendar/DAY_OF_MONTH) (.get calendar Calendar/DAY_OF_MONTH))
                                           (not= (.get birthday Calendar/YEAR) (.get calendar Calendar/YEAR))))
                                    commits)]
          (->> anniv-commits
            (group-by :email)
            (map (fn [[email commits]]
                   (let [level (->> commits
                                 (map #(.get (:calendar %) Calendar/YEAR))
                                 distinct count)]
                     {:commit-info (first commits)
                      :level level}))))))))

  (timeline-scanner :flash
    (fn [commits]
      (for [[email author-commits] (group-by :email commits)
            :let [ordered (sort-by :calendar #(compare %2 %1) author-commits) ; desc
                  commit  (->> (map vector ordered (next ordered))
                               (filter (fn [[commit before]]
                                         (< 0
                                            (- (.getTimeInMillis ^Calendar (:calendar commit))
                                               (.getTimeInMillis ^Calendar (:calendar before)))
                                            (* 15 1000))))
                               ffirst)]
            :when commit]
        {:commit-info commit})))

  (timeline-scanner :catchphrase
    (fn [commits & {:keys [threshold] :or {threshold 10}}]
      (->> commits
        (group-by (juxt :email :message))
        (filter (fn [[_ cs]] (<= threshold (count cs))))
        (map (fn [[_ cs]]
               {:commit-info (util/min-by :calendar cs)})))))

  (timeline-scanner :loneliness
    (fn [commits]
      (let [taxon (fn [^Calendar calendar]
                    [(.get calendar Calendar/YEAR) (.get calendar Calendar/MONTH)])
            current-month (taxon (Calendar/getInstance))
            lonely-authors (for [[tx month-commits] (group-by #(taxon (:calendar %)) commits)
                                 :when (not= current-month tx)
                                 :let [authored-commits (group-by :email month-commits)]
                                 :when (= 1 (count authored-commits))]
                             (first authored-commits))]
        (->> lonely-authors
          (reduce (fn [storage [email cs]]
                    (update-in storage [email] concat cs)) {})
          (map (fn [[_email cs]]
                 {:commit-info (util/min-by :calendar cs)}))))))

  (timeline-scanner :necromancer
    (fn [commits]
      (let [ordered (sort-by :calendar commits)
            necro? (fn [[before commit]]
                          (let [threshold (doto (.clone (:calendar commit))
                                                (.add Calendar/MONTH -1))]
                            (neg? (compare (:calendar before) threshold))))]
        (->> (map vector ordered (next ordered))
             (filter necro?)
             (map second)
             (group-by :email)
             (map (fn [[_email cs]]
                    {:commit-info (util/min-by :calendar cs)}))))))

  (timeline-scanner :combo
    (fn [commits & {:keys [streak] :or {streak 10}}]
      (let [combos (calculate-combos commits)]
        (->> combos
          (filter #(<= streak (second %)))
          (group-by (comp :email first))
          (util/map-vals (partial util/max-by second))
          (map (fn [[_email [commit combo-length]]]
                 {:commit-info commit
                  :level (quot combo-length streak)}))))))

  (timeline-scanner :combo-breaker
    (fn [commits & {:keys [streak] :or {streak 10}}]
      (let [combos (calculate-combos commits)
            sha->commit (util/map-by :id commits)
            parent->children (parentage commits)
            combo-breaks (for [child commits
                               parent (map sha->commit (:parents child))
                               :let [combo-length (combos parent)]
                               :when (->> (:id parent)
                                          (parent->children)
                                          (every? #(not= (:email %) (:email parent))))]
                           [child combo-length])]
        (->> combo-breaks
          (filter #(<= streak (second %)))
          (group-by (comp :email first))
          (util/map-vals (partial util/max-by second))
          (map (fn [[_email [commit combo-length]]]
                 {:commit-info commit
                  :level (quot combo-length streak)}))))))

)

  ;;  TODO
  ;;  commenter
  ;;  blamer
  ;;  waste
  ;;  collision
  ;;  peacemaker
  ;;  worker-bee
  ;;  oops
  ;;  [!] what-happened-here
  ;;  [!] all-things-die
  ;;  [!] goodboy
  ;;  commit-get


