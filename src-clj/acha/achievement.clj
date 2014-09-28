(ns acha.achievement
  (:use clojure.data)
  (:require
    [clojure.set :as set]
    [acha.util :as util]
    [acha.achievement-static]
    [acha.spellings :as spellings]
    [acha.swears :as swears]
    [acha.emoji :as emoji]
    [clojure.string :as str])
  (:import
    [java.util Calendar]))

; FIXME: All commit-info scanners return commit's time and author.
;        That's a lot of boilerplate.

(def example-achievement
  {:username "Bender"
   :level 3
   :time #inst "2014-04-16T17:43:20.000-00:00"})

(defn make-subword-scanner [[achievement-id words]]
  [achievement-id
   (fn [{:keys [message author time]}]
     (when (pos? (->> (clojure.string/split message #"\b") (filter words) (count)))
         {:username author
          :time time}))])

; months are zero-based because java
(defn make-date-scanner [[achievement-id month day]]
  [achievement-id
   (fn [commit-info]
     (let [time (:time commit-info)
           cal (Calendar/getInstance)
           _ (.setTime cal time)
           _ (.setTimeZone cal (:timezone commit-info))           
           commit-day (.get cal Calendar/DAY_OF_MONTH)
           commit-month (.get cal Calendar/MONTH)]
       (when (= [month day] [commit-month commit-day])
         {:username (:author commit-info)
          :time time})))])

(defn make-filename-scanner [[achievement-id filename-pred]]
  [achievement-id
   (fn [{:keys [changed-files author time]}]
     (let [added-files (->> changed-files
                            (filter #(= (first %) :add))
                            (map (comp :path last)))]
       (when (some filename-pred added-files)
         {:username author
          :time time})))])

(defn make-language-scanner [[achievement-id extensions]]
  (let [dot-extensions (map #(str "." %) extensions)
        has-interesting-extension?
        (fn [file] (some #(.endsWith file %) dot-extensions))]
    (make-filename-scanner [achievement-id has-interesting-extension?])))

(defn make-word-counting-scanner [[achievement-id words]]
  [achievement-id
   (fn [{:keys [message author time]}]
     (let [word-count (->> (clojure.string/split message #"\b") (filter words) (count))]
       (when (pos? word-count)
         {:level word-count
          :username author
          :time time})))])

(def bad-motherfucker
  (make-word-counting-scanner [:bad-motherfucker swears/table]))

(def hello-linus
  [:hello-linus 
  (fn [commit-info]
    (when-let [bad-acha ((second bad-motherfucker) commit-info)]
      (when (>= (:level bad-acha) 10)
        {:username (:author commit-info)
         :time     (:time commit-info)})))])

(def borat
  (make-word-counting-scanner [:borat spellings/table]))

(defn make-message-scanner [[achievement-id message-predicate]]
  [achievement-id
   (fn [{:keys [message author time]}]
     (when (message-predicate message)
       {:username author
        :time time}))])

(def leo-tolstoy
  (make-message-scanner
    [:leo-tolstoy
    #(>= (count (str/split-lines %)) 10)]))

(def man-of-few-words
  (make-message-scanner
    [:man-of-few-words
    #(< (count %) 4)]))

(def no-more-letters
  (make-message-scanner
    [:no-more-letters
    (fn [message]
      (not (some #(Character/isLetter (.charValue %)) message)))]))

(def narcissist
  [:narcissist
   (fn [{:keys [message author time]}]
       (when (.contains message author)
         {:username author
          :time time}))])

(def change-of-mind
  [:change-of-mind
   (fn [{:keys [changed-files author time]}]
     (let [edited-files (->> changed-files
                             (filter #(= (first %) :add))
                             (map (comp :path last)))]
       (when (some #{"LICENSE"} edited-files)
         {:username author
          :time time})))])

; sha achievements
(defn make-sha-scanner [[achievement-id sha-predicate]]
  [achievement-id
   (fn [{:keys [id author time]}]
     (when (sha-predicate id)
       {:username author
        :time time}))])

(def lucky
  (make-sha-scanner
    [:lucky
    #(.contains % "777")]))

(def mark-of-the-beast
  (make-sha-scanner
    [:mark-of-the-beast
    #(.contains % "666")]))

; LOC achievements
(defn make-loc-scanner [[achievement-id loc-predicate]]
  [achievement-id
   (fn [{:keys [loc author time]}]
     (when (loc-predicate loc)
       {:username author
        :time time}))])

(def world-balance
  (make-loc-scanner
    [:world-balance
    (fn [loc] (and (= (:added loc 0) (:deleted loc 0)) (> (:added loc 0) 0)))]))

(def eraser
  (make-loc-scanner
    [:eraser
    (fn [loc] (= 0 (:added loc 0)))]))

(def massive
  (make-loc-scanner
    [:massive
    (fn [loc] (>= (:added loc 0) 1000))]))

; diff achievements
(def easy-fix
  [:easy-fix
   (fn [{:keys [diffs author time]}]
     (let [two-line-swapped?
           (fn [diff]
             (and
               ; exactly two lines are changed
               (= (count diff) 2)
               ; one is added and one removed
               (= (count (filter #(= :add (first %)) diff)))
               ; content is the same
               (= ((first diff) 1) ((last diff) 1))
               ; line number absolute difference is 1
               (= 1 (Math/abs (- (last (first diff)) (last (last diff)))))))]
       (when (and (= (count diffs) 1) (some two-line-swapped? diffs))
         {:username author
          :time time})))])

(defn commit-info-cal [commit-info]
  (doto
    (Calendar/getInstance (:timezone commit-info))
    (.setTime (:time commit-info))))

(def programmers-day
  [:programmers-day
   (fn [commit-info]
     (let [cal        (commit-info-cal commit-info)
           commit-day (.get cal Calendar/DAY_OF_YEAR)]
       (when (= 256 commit-day)
         {:username (:author commit-info)
          :time     (.getTime cal)})))])

(def thanksgiving
  [:thanksgiving
   (fn [commit-info]
     (let [cal (commit-info-cal commit-info)
           commit-day       (.get cal Calendar/DAY_OF_WEEK)
           commit-month     (.get cal Calendar/MONTH)
           commit-month-day (.get cal Calendar/DAY_OF_MONTH)]
       (when (and (= 10 commit-month) ;; November
                  (= 5 commit-day) ;; Thursday
                  (<= 22 commit-month-day 28)) ;; 4th Thursday
         {:username (:author commit-info)
          :time     (.getTime cal)})))])

(def owl
  [:owl
   (fn [commit-info]
     (let [cal (commit-info-cal commit-info)
           commit-hour (.get cal Calendar/HOUR_OF_DAY)]
       (when (<= 4 commit-hour 7) ;; between 4am and 7am
         {:username (:author commit-info)
          :time     (.getTime cal)})))])

(def dangerous-game
  [:dangerous-game
   (fn [commit-info]
     (let [cal (commit-info-cal commit-info)
           commit-wday (.get cal Calendar/DAY_OF_WEEK)
           commit-hour (.get cal Calendar/HOUR_OF_DAY)]
       (when (and (>= commit-hour 18) ;; after 18PM
                  (= commit-wday 6))  ;; friday
         {:username (:author commit-info)
          :time     (.getTime cal)})))])

(def time-get
  [:time-get
   (fn [commit-info]
     (let [cal (commit-info-cal commit-info)
           commit-minute (.get cal Calendar/MINUTE)
           commit-hour (.get cal Calendar/HOUR_OF_DAY)]
       (when (and (= commit-hour 0) (= commit-minute 0))
         {:username (:author commit-info)
          :time     (.getTime cal)})))])

(def mover
  [:mover
   (fn [commit-info]
     (when (some #(= (first %) :copy) (:diffs commit-info))
       {:username (:author commit-info)
        :time     (:time commit-info)}))])

(def empty-commit
  [:empty-commit
   (fn [{:keys [changed-files author time]}]
     (when (= (count changed-files) 0)
       {:username author
        :time time}))])

(def wrecking-ball
  [:wrecking-ball
   (fn [{:keys [changed-files author time]}]
     (when (>= (count changed-files) 100)
       {:username author
        :time time}))])

(def cool-kid
  [:emoji
   (fn [{:keys [message author time]}]
     (when (or (re-find #"[\ud83c\udc00-\ud83d\udeff\udbb9\udce5-\udbb9\udcee]" message)
               (let [candidates (set (re-seq #"\:[\w0-9]+\:" message))]
                 (not-empty (set/intersection candidates emoji/all))))
         {:username author
          :time time}))])

(def alzheimers
  [:alzheimers
   (fn [{:keys [author time between-time]}]
     (when (>= between-time (* 60 60 24 30))
         {:username author
          :time time}))])

; TODO commit-info achievements
(def commenter
  [:commenter
   (fn [commit-info]
     nil)])
;; (def ocd
;;   [:ocd
;;    (fn [commit-info]
;;      nil)])
(def holy-war
  [:holy-war
   (fn [commit-info]
     nil)])
(def fat-ass
  [:fat-ass
   (fn [commit-info]
     nil)])
(def deal-with-it
  [:deal-with-it
   (fn [commit-info]
     nil)])
(def for-stallman
  [:for-stallman
   (fn [commit-info]
     nil)])

; TODO timeline achievements
(def blamer
  [:blamer
   (fn [timeline]
     nil)])
(def catchphrase
  [:catchphrase
   (fn [timeline]
     nil)])
(def anniversary
  [:anniversary
   (fn [timeline]
     nil)])
(def flash
  [:flash
   (fn [timeline]
     nil)])
(def waste
  [:waste
   (fn [timeline]
     nil)])
(def loneliness
  [:loneliness
   (fn [timeline]
     nil)])
(def necromancer
  [:necromancer
   (fn [timeline]
     nil)])
(def collision
  [:collision
   (fn [timeline]
     nil)])
(def hydra
  [:hydra
   (fn [timeline]
     nil)])
(def peacemaker
  [:peacemaker
   (fn [timeline]
     nil)])
(def combo
  [:combo
   (fn [timeline]
     nil)])
(def combo-breaker
  [:combo-breaker
   (fn [timeline]
     nil)])
(def worker-bee
  [:worker-bee
   (fn [timeline]
     nil)])
(def oops
  [:oops
   (fn [timeline]
     nil)])
(def what-happened-here
  [:what-happened-here
   (fn [timeline]
     nil)])
(def all-things-die
  [:all-things-die
   (fn [timeline]
     nil)])
(def goodboy
  [:goodboy
   (fn [timeline]
     nil)])
(def commit-get
  [:commit-get
   (fn [timeline]
     nil)])

; TODO meta achievements
(def gandalf nil)
(def munchkin nil)
(def unpretending nil)

(def date-table
  [[:christmas 11 25]
   [:halloween 9 31]
   [:fools-day 3 1]
   [:leap-day 1 29]
   [:new-year 0 1]
   [:russia-day 5 12]
   [:valentine 1 14]])

(def substring-table
  [[:beggar #{"achievement" "achievements"}]
   [:citation-needed #{"stackoverflow"}]
   [:fix #{"fix" "fixes" "fixed" "fixing"}]
   [:forgot #{"forgot"}]
   [:google #{"google"}]
   [:hack #{"hack"}]
   [:impossible #{"impossible"}]
   [:magic #{"magic"}]
   [:never-probably #{"later"}]
   [:secure #{"secure"}]
;;    [:sorry #{"sorry"}]
   [:wow #{"wow"}]])

(def language-table
  [[:basic ["bas" "vb" "vbs" "vba"]]
   [:c ["c"]]
   [:c-sharp ["cs"]]
   [:clojure ["clj" "cljx"]]
   [:clojurescript ["cljs"]]
   [:css ["css" "sass" "scss" "less" "haml"]]
   [:cxx ["c++" "cc" "cpp" "cxx" "pcc" "hh" "hpp" "hxx"]]
   [:dart ["dart"]]
   [:erlang ["erl" "hrl"]]
   [:go ["go"]]
   [:haskell ["hs" "lhs"]]
   [:java ["java" "jsf" "jsp" "jspf"]]
   [:javascript ["js"]]
   [:objective-c ["m" "mm"]]
   [:pascal ["pas"]]
   [:perl ["pl"]]
   [:php ["php" "php3" "php4" "php5"]]
   [:python ["py"]]
   [:ruby ["rake" "rb"]]
   [:scala ["scala"]]
   [:shell ["bash" "sh" "awk" "sed"]]
   [:sql ["sql"]]
   [:swift ["swift"]]
   [:windows-language ["bat" "btm" "cmd" "ps1" "csproj" "vbproj" "vcproj" "wdproj" "wixproj" "xaml"]]
   [:xml ["xml" "xsl" "xslt" "xsd" "dtd"]]])

(def multilingual
  [:multilingual
   (fn [commit-info]
     (when (>= (reduce + (map #(if (% commit-info) 1 0) 
      (map second (map make-language-scanner language-table)))) 5) 
       {:username (:author commit-info)
        :time (:time commit-info)}))])

(def filename-table
  [[:nothing-to-hide #(= % "id_rsa")]
   [:scribbler #(.startsWith (str/lower-case %) "readme")]
   ])

; Scanner is 2-tuple of name and scanning function
(def all-commit-info-scanners
  (concat
    (map make-filename-scanner filename-table)
    (map make-language-scanner language-table)
    (map make-date-scanner date-table)
    (map make-subword-scanner substring-table)
    [bad-motherfucker
     borat
     cool-kid
     eraser
     hello-linus
     leo-tolstoy
     man-of-few-words
     massive
     no-more-letters
     programmers-day
     thanksgiving
     owl
     easy-fix
     multilingual
     mover
     world-balance
     narcissist
     lucky
     mark-of-the-beast
     commenter
;;      ocd
     holy-war
     fat-ass
     deal-with-it
     dangerous-game
     empty-commit
     time-get
     for-stallman
     wrecking-ball
     alzheimers
     ]))

(def all-timeline-scanners
  [catchphrase
   anniversary
   blamer
   flash
   waste
   loneliness
   necromancer
   commit-get
   collision
   hydra
   peacemaker
   combo
   combo-breaker
   worker-bee
   oops
   what-happened-here
   all-things-die
   goodboy
   ])

(def all-meta-achievements
  [gandalf
   unpretending
   munchkin])

(def all-unused-achievements
  (diff (keys acha.achievement-static/table)
    (keys (into {} (concat 
      all-commit-info-scanners all-timeline-scanners all-meta-achievements)))))
