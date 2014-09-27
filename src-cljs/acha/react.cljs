(ns acha.react)

;; Inspired by https://github.com/levand/quiescent/blob/master/src/quiescent.cljs

(def ^:dynamic *component* nil)

(defn node []
  (.getDOMNode *component*))

(defn component [renderer & {:keys [will-update did-update]}]
  (let [react-component
        (.createClass js/React
           #js {:getInitialState (fn [] (atom {}))
                :render
                (fn []
                  (this-as this
                    (binding [*component* this]
                      (apply renderer (aget (.-props this) "args")))))
                :componentWillUpdate
                (fn [_ _]
                  (when will-update
                    (this-as this
                      (binding [*component* this]
                        (will-update (node))))))
                :componentDidUpdate
                (fn [_ _]
                  (when did-update
                    (this-as this
                      (binding [*component* this]
                        (did-update (node))))))
                })]
    (fn [& args]
      (react-component #js {:args args}))))

(defn render [component node]
  (.renderComponent js/React component node))

(defn remember [k v]
  (swap! (.-state *component*) assoc k v))

(defn recall [k]
  (get @(.-state *component*) k))
