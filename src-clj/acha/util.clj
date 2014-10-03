(ns acha.util
  (:require [clojure.string :as string])
  (:import [java.security MessageDigest]))

(defn md5
  "Compute the hex MD5"
  [^String s]
  (let [digest (doto (MessageDigest/getInstance "MD5") (.reset) (.update (.getBytes s)))]
    (apply str (map #(format "%02x" %) (.digest digest)))))

(defn normalize-str [str]
  (-> str string/trim string/lower-case))
