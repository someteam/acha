(ns acha.util
  (:require
     clojure.stacktrace
    [clojure.string :as string])
  (:import
    [java.security MessageDigest]))

(defn md5
  "Compute the hex MD5"
  [^String s]
  (let [digest (doto (MessageDigest/getInstance "MD5") (.reset) (.update (.getBytes s)))]
    (apply str (map #(format "%02x" %) (.digest digest)))))

(defn reason [throwable]
  (let [cause (clojure.stacktrace/root-cause throwable)]
    (str (.getName (type cause)) ": " (.getMessage cause))))

(defn normalize-str [str]
  (-> str string/trim string/lower-case))

(defn normalize-uri [uri]
  (let [u (.normalize (java.net.URI. uri))
        normalized  (java.net.URI.
                      (some-> (.getScheme u) string/lower-case)
                      (.getUserInfo u)
                      (some-> (.getHost u) string/lower-case)
                      (.getPort u)
                      (.getPath u)
                      (.getQuery u)
                      (.getFragment u))]
    (str normalized)))

(defn resolve-redirects
  ([url] (resolve-redirects url url 0 nil))
  ([original url counter cookies]
    (if (> counter 20)
      (throw (java.net.ProtocolException. (str "Too many redirects for " original ": " counter)))
      (let [conn (.openConnection (java.net.URL. url))]
        (try
          (doto conn
            (.setReadTimeout 10000)
            (.addRequestProperty "Accept-Language" "en-US,en;q=0.8")
            (.addRequestProperty "User-Agent" "Mozilla")
            (.addRequestProperty "Referer" "google.com"))
          (when cookies
            (.setRequestProperty "Cookie" cookies))
          (if (#{301 302 303} (.getResponseCode conn))
            (resolve-redirects original
                               (.getHeaderField conn "Location")
                               (inc counter)
                               (.getHeaderField conn "Set-Cookie"))
            (str url))
          (finally
            (.disconnect conn)))))))

(defn canonical-repo-url [url]
  (-> url
    string/trim
    normalize-uri
    (as-> url
      (if (or (.startsWith url "http://")
              (.startsWith url "https://"))
        (resolve-redirects url)
        url))))
