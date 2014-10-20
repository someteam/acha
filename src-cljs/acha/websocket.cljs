(ns acha.websocket
  (:require
    [cognitect.transit :as transit]
    [acha.util :as u]))

(defprotocol IWebSocket
  (send [payload]))

(defn resolve-url [url]
  (str "ws://" (.. js/window -location -host) url))

(defn reconnect [url ref {:keys [on-message on-close on-open interval]
                          :or {interval 1000}
                          :as opts}]
  (try
    (let [socket (js/WebSocket. url)]
      (set! (.-onopen socket) (fn [event]
        (.log js/console "[ websocket ] connected")
        (reset! ref socket)
        (when on-open (on-open))))
      (set! (.-onmessage socket) (fn [event]
;;         (.log js/console "DATA" event)
        (when on-message
          (on-message (u/read-transit (.-data event))))))
      (set! (.-onclose socket) (fn [event]
        (when @ref
          (.log js/console "[ websocket ] disconnected")
          (reset! ref nil)
          (when on-close (on-close)))
        (js/setTimeout (fn [] (reconnect url ref opts)) interval))))
    (catch js/Error error
      (reset! ref nil)
      (.log js/console "[ websocket ]" error)
      (js/setTimeout (fn [] (reconnect url ref opts)) interval))))
      
(defn connect [url & {:as opts}]
  (let [url (resolve-url url)
        ref (atom nil)]
    (reconnect url ref opts)))
