(ns acha.achievement
  (:require
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

; TODO commit-info achievements
(defn bad-motherfucker [commit-info] nil)
(defn hello-linus [commit-info] nil)
(defn man-of-few-words [commit-info] nil)
(defn leo-tolstoy [commit-info] nil)
(defn citation-needed [commit-info] nil)
(defn no-more-letters [commit-info] nil)
(defn cool-kid [commit-info] nil)
(defn beggar [commit-info] nil)
(defn borat [commit-info] nil)
(defn never-probably [commit-info] nil)
(defn professional-pride [commit-info] nil)
(defn turkey-day [commit-info] nil)

; TODO timeline achievements
(defn catchphrase [timeline] nil)
(defn anniversary [timeline] nil)

(def date-table
  [[:christmas 11 25]
   [:halloween 9 31]
   [:new-year 0 1]
   [:valentine 1 14]
   [:leap-day 1 29]
   [:russia-day 5 12]])

(def substring-table
  [[:impossible "impossible"]
   [:magic "magic"]
   [:sorry "sorry"]
   [:google "google"]
   [:forgot "forgot"]
   [:fix "fix"]
   [:secure "secure"]
   [:hack "hack"]
   [:wow "wow"]])

(def language-table
  [[:haskell ["hs" "lhs"]]
   [:perl ["pl"]]
   ])

; Scanner is 2-tuple of name and scanning function
(def all-commit-info-scanners
  (concat
    (map make-language-scanner language-table)
    (map make-date-scanner date-table)
    (map make-substring-scanner substring-table)
    [professional-pride
     turkey-day
     catchphrase
     bad-motherfucker
     hello-linus
     man-of-few-words
     leo-tolstoy
     citation-needed
     no-more-letters
     cool-kid
     beggar
     borat
     never-probably
     ]))

(def all-timeline-scanners
  [catchphrase
   anniversary
   ])

