(ns acha.dom
  (:require
    [goog.events :as events]
    [goog.history.EventType :as history.EventType]
    [clojure.string :as str]
    [acha.util :as u])
  (:import
    goog.history.Html5History)
  (:refer-clojure :exclude [parents]))

(defn parents [node]
  (lazy-seq (when node (cons node (parents (.-parentElement node))))))

(defn parent [node pred]
  (->> (parents node) (filter pred) first))

(defn attr [node attr]
  (.getAttribute node (name attr)))

(defn node-name [node]
  (str/lower-case (.-nodeName node)))

(defn scroll-to-top
  ([]
    (when (> (.-scrollY js/window) 190) ;; ~height of header
      (scroll-to-top 30)))
  ([delta]
    (let [y (.-scrollY js/window)]
      (when (> y 0)
        (.scrollBy js/window 0 (- (min y delta)))
        (js/setTimeout #(scroll-to-top (* 1.1 delta)) 16)))))

;; Navigation

(def ^:private history (Html5History.))

(def ^:dynamic *title-suffix* "Acha-acha")

(defn set-title! [title]
  (set! (.-title js/document) (if title (str title " — " *title-suffix*) *title-suffix*)))

(defn go! [& path]
  (scroll-to-top)
  (.setToken history (apply str path)))

(defn listen-nav [callback]
  (doto history
    (events/listen history.EventType/NAVIGATE
      (fn [e]
        (callback (.-token e))))
    (.setUseFragment true)
    (.setPathPrefix "#")
    (.setEnabled true)))
