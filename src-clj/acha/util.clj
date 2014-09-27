(ns acha.util
  (:require [clojure.string :as string])
  (:import [java.security MessageDigest]))

(defn md5
  "Compute the hex MD5"
  [^String s]
  (let [digest (doto (MessageDigest/getInstance "MD5") (.reset) (.update (.getBytes s)))]
    (apply str (map #(format "%02x" %) (.digest digest)))))

(defn count-substring-occurrences [haystack needle]
  (count (re-seq (re-pattern needle) haystack)))

(defn normalize-str [str]
  (-> str string/trim string/lower-case))

(defn ^java.text.SimpleDateFormat create-dateformat
  ([] (create-dateformat "yyyy-MM-dd'T'HH:mm:ss'Z'"))
  ([^String format]
    (doto
      (java.text.SimpleDateFormat. format (java.util.Locale. "en"))
      (.setTimeZone (java.util.TimeZone/getTimeZone "GMT")))))

(defn format-date
  ([date] (format-date date (create-dateformat)))
  ([date  ^java.text.SimpleDateFormat format] (.format format date)))
