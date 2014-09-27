(ns acha.util
  (:import [java.security MessageDigest]))

(defn md5
  "Compute the hex MD5"
  [^String s]
  (let [digest (doto (MessageDigest/getInstance "MD5") (.reset) (.update (.getBytes s)))]
    (apply str (map #(format "%02x" %) (.digest digest)))))
