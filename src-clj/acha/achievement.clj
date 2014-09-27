(ns acha.achievement
  (:require
    [clojure.string :as str]))

(def example-achievement
  {:name "Shiny Metal Ass"
   :username "Bender"
   :level 3
   :timestamp 1234567890})

(defn substring-phrasing-scanner [name needle]
  (fn [commit-info]
    (let [l-needle (str/lower-case needle)
          l-msg    (str/lower-case (:message commit-info))]
    (when (.contains l-msg l-needle)
      {:name name
       :username (:author-name commit-info)
       :timestamp (:timestamp commit-info)}))))

; TODO commit-info achievements
(defn catchphrase [timeline] nil)
(defn bad-motherfucker [timeline] nil)
(defn hello-linus [timeline] nil)
(defn man-of-few-words [timeline] nil)
(defn leo-tolstoy [timeline] nil)
(defn citation-needed [timeline] nil)
(defn no-more-letters [timeline] nil)
(defn cool-kid [timeline] nil)
(defn beggar [timeline] nil)
(defn borat [timeline] nil)
(defn never-probably [timeline] nil)

; TODO diff achievements

; TODO timeline achievements
(defn catchphrase [timeline]
  nil)

; All the scanners return either nil or an achievement

(def all-diff-scanners
  [])

(def all-commit-info-scanners
  [(substring-phrasing-scanner "Mission impossible" "impossible")
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
   ])

