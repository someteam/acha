(ns acha.core
  (:require
    [clojure.core.async :as async]))

(def working-dir ".acha")

(def events (async/chan))
(def events-mult (async/mult events))
