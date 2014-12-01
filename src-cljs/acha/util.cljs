(ns acha.util
  (:require
    [datascript :as d]
    [datascript.core :as dc]
    [cognitect.transit :as transit])
  (:require-macros
    [acha :refer [profile]]))

(defn datom [[e a v tx added]]
  (dc/Datom. e a v (or tx d/tx0) (if (nil? added) true added)))

(defn read-transit [s]
  (transit/read (transit/reader :json {:handlers {"datascript/Datom" datom}}) s))

(defn write-transit [s]
  (transit/write (transit/writer :json) s))

(defn- ajax [url callback & [method]]
  (.send goog.net.XhrIo url
    (fn [reply]
      (let [res (.getResponseText (.-target reply))
            res (profile (str "read-transit " url " (" (count res) " bytes)") (read-transit res))]
        (when callback
          (js/setTimeout #(callback res) 0))))
    (or method "GET")))

(defn map-by [f xs]
  (reduce (fn [acc x] (assoc acc (f x) x)) {} xs))

(defn trimr [s suffix]
  (let [pos (- (count s) (count suffix))]
    (if (and (>= (count s) (count suffix))
             (= (subs s pos) suffix))
      (subs s 0 pos)
      s)))

(defn starts-with? [s prefix]
  (= prefix (subs s 0 (count prefix))))

;; DATASCRIPT

(def ^:dynamic *debug-q* false)

(defn ent [db eid]
  (when eid
    (d/entity db eid)))

(defn -q [q & args]
  (if *debug-q*
    (let [key (str q)
          _   (.time js/console key)
          res (apply d/q q args)
          _   (.timeEnd js/console key)]
      res)
    (apply d/q q args)))

(defn q1
  "Return first element of first tuple of result"
  [q & args]
  (->> (apply -q q args) ffirst))

(defn q1-by
  "Return single entity id by attribute existence or attribute value"
  ([db attr]
    (-> (d/datoms db :aevt attr) first :e))
  ([db attr value]
    (-> (d/datoms db :avet attr value) first :e)))

(defn q1s
  "Return seq of first elements of each tuple"
  [q & args]
  (->> (apply -q q args) (map first)))

(defn qe
  "If queried entity id, return single entity of first result"
  [q db & sources]
  (->> (apply -q q db sources)
       ffirst
       (ent db)))

(defn qes
  "If queried entity ids, return all entities of result"
  [q db & sources]
  (->> (apply -q q db sources)
       (map #(ent db (first %)))))

(defn qe-by
  "Return single entity by attribute existence or specific value"
  ([db attr]
    (ent db(q1-by db attr)))
  ([db attr value]
    (ent db (q1-by db attr value))))

(defn qes-by
  "Return all entities by attribute existence or specific value"
  ([db attr]
    (map #(ent db (.-e %)) (d/datoms db :aevt attr)))
  ([db attr value]
    (map #(ent db (.-e %)) (d/datoms db :avet attr value))))

(defn qmap
  "Convert returned 2-tuples to a map"
  [q & sources]
  (into {} (apply -q q sources)))
