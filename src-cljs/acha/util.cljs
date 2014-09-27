(ns acha.util
  (:require
    [datascript :as d]))

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
    (map #(ent db (.-e %)) (d/datoms db :aevt attr value))))

(defn qmap
  "Convert returned 2-tuples to a map"
  [q & sources]
  (into {} (apply -q q sources)))
