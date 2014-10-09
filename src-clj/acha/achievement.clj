(ns acha.achievement
  (:use clojure.data)
  (:require
    [clojure.set :as set]
    [acha.util :as util]
    [acha.achievement-static]
    [acha.spellings :as spellings]
    [acha.swears :as swears]
    [acha.emoji :as emoji]
    [clojure.string :as string])
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
     (when (pos? (->> (string/split message #"\b") (filter words) (count)))
         {:username author
          :time time}))])

(defn commit-info-cal [commit-info]
  (doto
    (Calendar/getInstance (:timezone commit-info))
    (.setTime (:time commit-info))))

; months are zero-based because java
(defn make-date-scanner [[achievement-id month day]]
  [achievement-id
   (fn [commit-info]
     (let [cal          (commit-info-cal commit-info)
           commit-day   (.get cal Calendar/DAY_OF_MONTH)
           commit-month (+ 1 (.get cal Calendar/MONTH))]
       (when (and (= month commit-month)
                  (= day commit-day))
         {:username (:author commit-info)
          :time     (:time commit-info)})))])

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
     (let [word-count (->> (string/split message #"\b") (filter words) (count))]
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
    #(>= (count (string/split-lines %)) 10)]))

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
    (fn [loc]
      (and (= (:added loc 0) (:deleted loc 0))
           (pos? (:added loc 0))))]))

(def eraser
  (make-loc-scanner
    [:eraser
    (fn [loc]
      (and
        (= 0 (:added loc 0))
        (pos? (:deleted loc 0))))]))

(def massive
  (make-loc-scanner
    [:massive
    (fn [loc] (>= (:added loc 0) 1000))]))

; diff achievements
(def easy-fix
  [:easy-fix
   (fn [{:keys [changed-files author time]}]
       (when (and
                  ; only one file was changed
                  (= (count changed-files) 1)
                  (let [diff (get-in changed-files [0 :diff])]
                    (or
                      ; swap two nonadjacent lines
                      (and
                        (= 2 (count diff))
                        ; not blank lines
                        (not (string/blank? (get-in diff [0 :added 0])))
                        (not (string/blank? (get-in diff [1 :added 0])))
                        ; content added in one edit == removed in another edit
                        (= (get-in diff [0 :added 0])   (get-in diff [1 :removed 0]))
                        (= (get-in diff [1 :added 0])   (get-in diff [0 :removed 0])))

                      ; swap two adjacent lines
                      (and
                        (= 1 (count diff))
                        ; not blank line
                        (not (string/blank? (get-in diff [0 :added 0])))
                        ; the smae
                        (= (get-in diff [0 :added 0])   (get-in diff [0 :removed 0]))
                        ; adjacent
                        (= 1 (Math/abs (- (get-in diff [0 :added 1])
                                          (get-in diff [0 :removed 1]))))))))
         {:username author
          :time time}))])

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
   (fn [{:keys [changed-files author time]}]
     (when (some #(and (= (:kind %) :rename)
                       (= 0 (count (:diff %)))) changed-files)
       {:username author
        :time     time}))])

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
  [[:christmas 12 25]
   [:halloween 10 31]
   [:fools-day 4 1]
   [:leap-day 2 29]
   [:new-year 1 1]
   [:russia-day 6 12]
   [:valentine 2 14]])

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
;;    [:c ["c" "h"]]
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
   [:scribbler #(.startsWith (string/lower-case %) "readme")]
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
