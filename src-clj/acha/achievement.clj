(ns acha.achievement
  (:require
    [clojure.string :as str])
  (:import [java.util Calendar]))

(def example-achievement
  {:name "Shiny Metal Ass"
   :username "Bender"
   :level 3
   :time #inst "2014-04-16T17:43:20.000-00:00"})

(defn substring-phrasing-scanner [name needle]
  (fn [commit-info]
    (let [l-needle (str/lower-case needle)
          l-msg    (str/lower-case (:message commit-info))]
    (when (.contains l-msg l-needle)
      {:name name
       :username (:author commit-info)
       :time (:time commit-info)}))))

; months are zero-based because java
(defn date-scanner [name month day]
  (fn [commit-info]
    (let [time (:time commit-info)
          cal (Calendar/getInstance)
          _ (.setTime cal time)
          commit-day (.get cal Calendar/DAY_OF_MONTH)
          commit-month (.get cal Calendar/MONTH)
          _ (print "XXX" commit-day commit-month)]
      (when (= [month day] [commit-month commit-day])
        {:name name
         :username (:author commit-info)
         :time time}))))

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

; TODO diff achievements

; TODO timeline achievements
(defn catchphrase [timeline] nil)
(defn anniversary [timeline] nil)

; All the scanners return either nil or an achievement

(def all-diff-scanners
  [])

(def all-commit-info-scanners
  [professional-pride
   turkey-day
   (date-scanner "Ruined Christmas" 11 25)
   (date-scanner "This code looks scary" 9 31)
   (date-scanner "New year, new bugs" 0 1)
   (date-scanner "In love with work" 1 14)
   (date-scanner "Rare occasion" 1 29)
   (date-scanner "From Russia with Love" 5 12)
   (substring-phrasing-scanner "Mission impossible" "impossible")
   (substring-phrasing-scanner "The Colour of Magic" "magic")
   (substring-phrasing-scanner "Salvation" "sorry")
   (substring-phrasing-scanner "I can sort it out myself" "google")
   (substring-phrasing-scanner "Second thoughts" "forgot")
   (substring-phrasing-scanner "Save the day" "fix")
   (substring-phrasing-scanner "We're safe now" "secure")
   (substring-phrasing-scanner "We're safe now" "secure")
   (substring-phrasing-scanner "1337 H4XX0R" "hack")
   (substring-phrasing-scanner "Wow" "wow")
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
   ])

(def all-timeline-scanners
  [catchphrase
   anniversary
   ])

