(ns acha.react
  (:require
    [sablono.core :as s]))

(defmacro defc [name argvec render & rest]
  `(def ~name (component (fn ~argvec (s/html ~render)) ~@rest)))
