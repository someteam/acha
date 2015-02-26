(ns acha.util
  (:require
     clojure.stacktrace
    [cognitect.transit :as transit]
    [clojure.string :as string])
  (:import
    [java.util Calendar TimeZone]
    [java.io ByteArrayOutputStream ByteArrayInputStream]
    [java.security MessageDigest]))

(defn create-calendar 
  (^Calendar [time] 
    (create-calendar time (TimeZone/getTimeZone "UTC")))
  (^Calendar [time timezone]
    (doto
      (Calendar/getInstance timezone)
      (.setTime time))))

(defn write-transit-bytes [x]
  (let [baos (ByteArrayOutputStream.)
        w    (transit/writer baos :json {})]
    (transit/write w x)
    (.toByteArray baos)))

(defn write-transit-str [x]
  (String. ^bytes (write-transit-bytes x) "UTF-8"))

(defn read-transit-str
  "Reads a value from a decoded string"
  [^String s]
    (let [in (ByteArrayInputStream. (.getBytes s "UTF-8"))]
      (transit/read (transit/reader in :json))))

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

(defn- normalize-scheme-uri [uri]
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

(defn- normalize-schemeless-uri [uri]
  (when-let [[_ user host path] (re-matches #"(.+)@([\w\d\.]+):(.*)" uri)]
    (str user "@" (normalize-str host) ":" path)))

(defn normalize-uri [uri]
  (let [normalized (if (re-find #"^(\w+://)" uri)
                     (normalize-scheme-uri uri)
                     (normalize-schemeless-uri uri))]
    (if-not (string/blank? normalized)
      normalized
      (throw (ex-info "Unknown uri format" {:uri uri})))))

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
              (.startsWith url "https://")
              (.startsWith url "git@"))
        (resolve-redirects url)
        url))))

(defn mapmap [f m]
  (into {} (map (fn [[k v]] (f k v)) m)))

(defn map-keys [f m]
  (mapmap (fn [k v] [(f k) v]) m))

(defn map-vals [f m]
  (mapmap (fn [k v] [k (f v)]) m))

(defn map-by [key-fn coll]
  (->> coll (map #(vector (key-fn %) %)) (into {})))

(defn min-by [f [x & xs]]
  (reduce #(if (neg? (compare (f %1) (f %2))) %1 %2) x xs))

(defn max-by [f [x & xs]]
  (reduce #(if (pos? (compare (f %1) (f %2))) %1 %2) x xs))

(defn -reduce
  "Variant of reduce that does not unwrap (reduced)"
  [f init coll]
  (reduce #(let [result (f %1 %2)]
                (cond-> result
                  ;; wrap twice because reduce will unwrap one reduced
                  ;; but we want to pass that info down the line
                  (reduced? result) reduced))
           init coll))

(defn -reduce-kv
  "Variant of reduce-kv that does not unwrap (reduced)"
  [f init coll]
  (reduce-kv #(let [result (f %1 %2 %3)]
                (cond-> result
                  ;; wrap twice because reduce-kv will unwrap one reduced
                  ;; but we want to pass that info down the line
                  (reduced? result) reduced))
             init coll))

