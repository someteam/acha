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

(defmacro commit-scanner
  [scanners sname args & body]
  `(update-in ~scanners [:commit-scanners] assoc ~sname
     (fn ~args
       (when-let [ret# (do ~@body)]
         (if (map? ret#) ret# {})))))

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

(defn create-calendar ^Calendar [time timezone]
  (doto
    (Calendar/getInstance timezone)
    (.setTime time)))

(defn- date-scanner [scanners sname month day]
  (commit-scanner scanners sname [{:keys [time timezone]}]
   (let [calendar (create-calendar time timezone)]
     (and (= month (+ 1 (.get calendar Calendar/MONTH)))
          (= day (.get calendar Calendar/DAY_OF_MONTH))))))

(defn- subword-scanner [scanners sname words]
  (commit-scanner scanners sname [commit-info]
    (pos? (scan-word-count (:message commit-info) words))))

(defn- scan-filenames [rexps changed-files kinds]
  (let [new-paths (->> changed-files
                    (filter #(kinds (:kind %)))
                    (map (comp :path :new-file)))]
    (some (fn [[p e]] (re-matches e p))
          (for [p new-paths, e rexps] [p e]))))

(defn- language-scanner [scanners sname exts]
  (let [rexps (map #(re-pattern (str "(?i)(?:.+)\\." % "$")) exts)]
    (-> scanners
      (update-in [:languages] assoc sname rexps)
      (commit-scanner sname [{:keys [changed-files]}]
        (scan-filenames rexps changed-files #{:add})))))

(defn- check-changed-lines [f changed-files]
  (some->> (for [{:keys [diff]} changed-files
                 {:keys [added removed]} diff]
             (when (and (not= 0 (count added))
                        (= (count added) (count removed)))
               (every? (fn [[[l1 _] [l2 _]]] (f l1 l2))
                       (map vector added removed))))
           not-empty
           (every? identity)))

(defscanners base
  (commit-scanner :bad-motherfucker [{:keys [message]}]
    (let [word-count (scan-word-count message swears/table)]
      (when (pos? word-count) {:level word-count})))

  (commit-scanner :hello-linus [commit-info]
    (when-let [a ((get-in base [:commit-scanners :bad-motherfucker]) commit-info)]
      (<= 5 (:level a))))

  (commit-scanner :borat [{:keys [message]}]
    (let [word-count (scan-word-count message spellings/table)]
      (when (pos? word-count) {:level word-count})))

  (commit-scanner :leo-tolstoy [{:keys [message]}]
    (<= 10 (-> message string/split-lines count)))

  (commit-scanner :man-of-few-words [{:keys [message]}]
    (< (count message) 4))

  (commit-scanner :no-more-letters [{:keys [^String message]}]
    (not (some #(Character/isLetter (.charValue %)) message)))

  (commit-scanner :narcissist [{:keys [^String message author]}]
    (.contains (util/normalize-str message)
               (util/normalize-str author)))

  ;; SHA achievements
  (commit-scanner :lucky [{:keys [^String id]}]
    (.contains id "777"))

  (commit-scanner :mark-of-the-beast [{:keys [^String id]}]
    (.contains id "666"))

  ;; LOC achievements
  (commit-scanner :world-balance [{:keys [changed-files]}]
    (let [loc (calculate-loc changed-files (filter #(not= (:added %) (:removed %))))]
       (and (pos? (:added loc))
            (= (:added loc) (:removed loc)))))

  (commit-scanner :eraser [{:keys [changed-files]}]
    (let [loc (calculate-loc changed-files)]
      (and (= 0 (:added loc)) (pos? (:removed loc)))))

  (commit-scanner :massive [{:keys [changed-files]}]
    (let [loc (calculate-loc changed-files)]
      (<= 1000 (:added loc))))

  ;; DIFF achievements
  (commit-scanner :easy-fix [{:keys [changed-files]}]
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
                              (get-in diff [0 :removed 0 1])))))))))

  ;; COMMIT DATE AND TIME
  (commit-scanner :programmers-day [{:keys [time timezone]}]
    (let [calendar (create-calendar time timezone)]
      (= 256 (.get calendar Calendar/DAY_OF_YEAR))))

  (commit-scanner :thanksgiving [{:keys [time timezone]}]
    (let [calendar (create-calendar time timezone)]
       (and (= 10 (.get calendar Calendar/MONTH)) ;; November
            (= 5 (.get calendar Calendar/DAY_OF_WEEK)) ;; Thursday
            (<= 22 (.get calendar Calendar/DAY_OF_MONTH) 28)))) ;; 4th Thursday

  (commit-scanner :owl [{:keys [time timezone]}]
    (let [calendar (create-calendar time timezone)]
       (<= 4 (.get calendar Calendar/HOUR_OF_DAY) 7))) ;; between 4am and 7am

  (commit-scanner :dangerous-game [{:keys [time timezone]}]
    (let [calendar (create-calendar time timezone)]
      (and (<= 18 (.get calendar Calendar/HOUR_OF_DAY)) ;; after 18PM
           (= 6 (.get calendar Calendar/DAY_OF_WEEK))))) ;; Friday

  (commit-scanner :time-get [{:keys [time timezone]}]
    (let [calendar (create-calendar time timezone)]
      (and (= 0 (.get calendar Calendar/HOUR_OF_DAY))
           (= 0 (.get calendar Calendar/MINUTE)))))

  (commit-scanner :alzheimers [{:keys [between-time]}]
    (<= (* 60 60 24 30) between-time))

  (commit-scanner :mover [{:keys [changed-files]}]
    (some #(and (= (:kind %) :rename)
                (= 0 (count (:diff %)))) changed-files))

  ;; ASK. It's not possible to have a commit without message or without files (except merges)
  (commit-scanner :empty-commit [{:keys [changed-files]}]
    (= (count changed-files) 0))

  (commit-scanner :wrecking-ball [{:keys [changed-files]}]
    (<= 100 (count changed-files)))

  (commit-scanner :emoji [{:keys [message]}]
    (or (re-find #"[\ud83c\udc00-\ud83d\udeff\udbb9\udce5-\udbb9\udcee]" message)
        (let [candidates (set (re-seq #"\:[\w0-9]+\:" message))]
          (not-empty (set/intersection candidates emoji/all)))))

  (commit-scanner :fat-ass [{:keys [changed-files]}]
    (some #(and (= :add (:kind %))
                (<= (* 2 1024 1024) (get-in % [:new-file :size] 0)))
          changed-files))

  (commit-scanner :holy-war [{:keys [changed-files]}]
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
      (check-changed-lines war? changed-files)))

  (commit-scanner :ocd [{:keys [changed-files]}]
    (let [ocd? (fn [^String line-a ^String line-b]
                 (and 
                   ;; line-b is a part of line-a
                   (.startsWith line-a line-b)
                   ;; the rest part contains spaces
                   (re-matches #"^[ \t]+$" (.substring line-a (.length line-b)))))]
      (check-changed-lines #(ocd? %2 %1) changed-files)))

  (date-scanner :christmas 12 25)
  (date-scanner :halloween 10 31)
  (date-scanner :fools-day 4 1)
  (date-scanner :leap-day 2 29)
  (date-scanner :new-year 1 1)
  (date-scanner :russia-day 6 12)
  (date-scanner :valentine 2 14)

  (commit-scanner :citation-needed [{:keys [message]}]
    (re-find #"(?i)(?:http(?:s)?:\/\/)?(?:www\.)?stackoverflow\.com" message))

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

  (language-scanner :basic ["bas" "vb" "vbs" "vba"])
  (language-scanner :c-sharp ["cs"])
  (language-scanner :clojure ["clj" "cljx"])
  (language-scanner :clojurescript ["cljs"])
  (language-scanner :css ["css" "sass" "scss" "less" "haml"])
  (language-scanner :cxx ["c++" "cc" "cpp" "cxx" "pcc" "hh" "hpp" "hxx"])
  (language-scanner :dart ["dart"])
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
  (language-scanner :sql ["sql"])
  (language-scanner :swift ["swift"])
  (language-scanner :windows-language ["bat" "btm" "cmd" "ps1" "csproj" "vbproj" "vcproj" "wdproj" "wixproj" "xaml"])
  (language-scanner :xml ["xml" "xsl" "xslt" "xsd" "dtd"])

  (commit-scanner :nothing-to-hide [{:keys [changed-files]}]
    (scan-filenames [#"(?i)(?:.*)id_rsa$"] changed-files #{:add}))

  (commit-scanner :scribbler [{:keys [changed-files]}]
    (scan-filenames [#"(?i)^readme(\.md)?$"] changed-files #{:add}))

  (commit-scanner :change-of-mind [{:keys [changed-files]}]
    (scan-filenames [#"(?i)^license(\.md)?$"] changed-files #{:edit}))

  (commit-scanner :multilingual [{:keys [changed-files]}]
    (let [level (->> (get-in base [:languages])
                   (filter #(scan-filenames (second %) changed-files #{:add :edit}))
                   count)]
      (when (<= 3 level)
        {:level level}))))

  ;;  TODO
  ;;  commenter
  ;;  holy-war
  ;;  deal-with-it
  ;;  for-stallman
  ;;  blamer
  ;;  catchphrase
  ;;  anniversary
  ;;  flash
  ;;  waste
  ;;  loneliness
  ;;  necromancer
  ;;  collision
  ;;  hydra
  ;;  peacemaker
  ;;  combo-breaker
  ;;  combo
  ;;  worker-bee
  ;;  oops
  ;;  what-happened-here
  ;;  all-things-die
  ;;  goodboy
  ;;  commit-get


