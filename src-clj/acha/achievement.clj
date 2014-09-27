(ns acha.achievement
  (:require
    [acha.util :as util]
    [clojure.string :as str])
  (:import [java.util Calendar]))

(def example-achievement
  { ; this keyword can be used to look up name, description and picture
    ; using achievement-static/table
   :keyword :shiny-metal-ass
   :username "Bender"
   :level 3
   :time #inst "2014-04-16T17:43:20.000-00:00"})

(defn make-substring-scanner [achievement-id needle]
  [achievement-id
   (fn [commit-info]
     (let [l-needle (str/lower-case needle)
           l-msg    (str/lower-case (:message commit-info))]
       (when (.contains l-msg l-needle)
         {:username (:author commit-info)
          :time (:time commit-info)})))])

; months are zero-based because java
(defn make-date-scanner [achievement-id month day]
  [achievement-id
   (fn [commit-info]
     (let [time (:time commit-info)
           ; TODO Correct timezone
           cal (Calendar/getInstance)
           _ (.setTime cal time)
           commit-day (.get cal Calendar/DAY_OF_MONTH)
           commit-month (.get cal Calendar/MONTH)]
       (when (= [month day] [commit-month commit-day])
         {:username (:author commit-info)
          :time time})))])

(defn make-language-scanner [achievement-id extensions]
  [achievement-id
   (fn [commit-info]
     nil)])

(def bad-motherfucker
  [:bad-motherfucker
   (fn [{:keys [message author time]}]
     (let [fuck-count (util/count-substring-occurrences message "fuck")]
       (when (pos? fuck-count)
         [{:level fuck-count
           :username author
           :time time}])))])

(def swear-words
  ["fuck" "shit" "damn" "sucks"])

(def hello-linus
  [:hello-linus
   (fn [{:keys [message author time]}]
     (let [count-word (fn [word] (util/count-substring-occurrences message word))
           swear-count (reduce + (map count-word swear-words))]
       (when (pos? swear-count)
         [{:level swear-count
           :username author
           :time time}])))])

; TODO commit-info achievements
(def borat
  [:borat
   (fn [commit-info]
     nil)])
(def catchphrase
  [:catchphrase
   (fn [commit-info]
     nil)])
(def citation-needed
  [:citation-needed
   (fn [commit-info]
     nil)])
(def cool-kid
  [:cool-kid
   (fn [commit-info]
     nil)])
(def eraser
  [:eraser
   (fn [commit-info]
     nil)])
(def leo-tolstoy
  [:leo-tolstoy
   (fn [commit-info]
     nil)])
(def man-of-few-words
  [:man-of-few-words
   (fn [commit-info]
     nil)])
(def massive
  [:massive
   (fn [commit-info]
     nil)])
(def never-probably
  [:never-probably
   (fn [commit-info]
     nil)])
(def no-more-letters
  [:no-more-letters
   (fn [commit-info]
     nil)])
(def professional-pride
  [:professional-pride
   (fn [commit-info]
     nil)])
(def turkey-day
  [:turkey-day
   (fn [commit-info]
     nil)])
(def scribbler
  [:scribbler
   (fn [commit-info]
     nil)])
(def owl
  [:owl
   (fn [commit-info]
     nil)])
(def easy-fix
  [:easy-fix
   (fn [commit-info]
     nil)])
(def multilingual
  [:multilingual
   (fn [commit-info]
     nil)])
(def mover
  [:mover
   (fn [commit-info]
     nil)])
(def world-balance
  [:world-balance
   (fn [commit-info]
     nil)])
(def narcissist
  [:narcissist
   (fn [commit-info]
     nil)])
(def blamer
  [:blamer
   (fn [commit-info]
     nil)])
(def lucky
  [:lucky
   (fn [commit-info]
     nil)])
(def mark-of-the-beast
  [:mark-of-the-beast
   (fn [commit-info]
     nil)])
(def commenter
  [:commenter
   (fn [commit-info]
     nil)])
(def ocd
  [:ocd
   (fn [commit-info]
     nil)])
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
(def dangerous-game
  [:dangerous-game
   (fn [commit-info]
     nil)])
(def empty-commit
  [:empty-commit
   (fn [commit-info]
     nil)])
(def time-get
  [:time-get
   (fn [commit-info]
     nil)])
(def for-stallman
  [:for-stallman
   (fn [commit-info]
     nil)])
(def change-of-mind
  [:change-of-mind
   (fn [commit-info]
     nil)])
(def wrecking-ball
  [:wrecking-ball
   (fn [commit-info]
     nil)])
(def alzheimers
  [:alzheimers
   (fn [commit-info]
     nil)])
(def nothing-to-hide
  [:nothing-to-hide
   (fn [commit-info]
     nil)])

; TODO timeline achievements
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
(def commit-get
  [:commit-get
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

; TODO meta achievements
(def gandalf nil)
(def munchkin nil)
(def unpretending nil)

(def date-table
  [[:christmas 11 25]
   [:halloween 9 31]
   [:leap-day 1 29]
   [:new-year 0 1]
   [:russia-day 5 12]
   [:valentine 1 14]])

(def substring-table
  [[:fix "fix"]
   [:forgot "forgot"]
   [:google "google"]
   [:hack "hack"]
   [:impossible "impossible"]
   [:magic "magic"]
   [:secure "secure"]
   [:sorry "sorry"]
   [:beggar "achievement"]
   [:wow "wow"]])

(def language-table
  [[:basic ["bas" "vb" "vbs" "vba"]]
   [:c ["c"]]
   [:c-sharp ["cs"]]
   [:clojure ["clj" "cljx"]]
   [:clojurescript ["cljs"]]
   [:css ["css" "sass" "scss" "less" "haml"]]
   [:cxx ["c++" "cc" "cpp" "cxx" "pcc" "hh" "hpp" "hxx"]]
   [:dart ["dart"]]
   [:duct-tape ["bash" "sh" "awk" "sed"]]
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
   [:sql ["sql"]]
   [:swift ["swift"]]
   [:windows-language ["bat" "btm" "cmd" "ps1" "csproj" "vbproj" "vcproj" "wdproj" "wixproj" "xaml"]]
   [:xml ["xml" "xsl" "xslt" "xsd" "dtd"]]])

; Scanner is 2-tuple of name and scanning function
(def all-commit-info-scanners
  (concat
    (map make-language-scanner language-table)
    (map make-date-scanner date-table)
    (map make-substring-scanner substring-table)
    [bad-motherfucker
     borat
     catchphrase
     citation-needed
     cool-kid
     eraser
     hello-linus
     leo-tolstoy
     man-of-few-words
     massive
     never-probably
     no-more-letters
     professional-pride
     turkey-day
     scribbler
     owl
     easy-fix
     multilingual
     mover
     world-balance
     narcissist
     blamer
     lucky
     mark-of-the-beast
     commenter
     ocd
     holy-war
     fat-ass
     deal-with-it
     dangerous-game
     empty-commit
     time-get
     for-stallman
     change-of-mind
     wrecking-ball
     alzheimers
     nothing-to-hide
     ]))

(def all-timeline-scanners
  [catchphrase
   anniversary
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
