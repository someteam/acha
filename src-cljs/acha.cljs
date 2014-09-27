(ns acha
  (:require
    [acha.react :as r :include-macros true]
    [sablono.core :as s]
    [secretary.core :as secretary :include-macros true :refer [defroute]]
    [goog.events :as events]
    [goog.history.EventType :as EventType])
  (:import
    goog.history.Html5History
    goog.net.XhrIo))

(enable-console-print!)
(declare index-page user-page)

(def users (atom {}))

;; Utils

(defn- ajax [url callback]
  (.send goog.net.XhrIo url
    (fn [reply]
      (-> (.-target reply)
          (.getResponseText)
          (->> (.parse js/JSON))
          (js->clj :keywordize-keys true)
          (callback)))))

(defn map-by [f xs]
  (reduce (fn [acc x] (assoc acc (f x) x)) {} xs))

;; Navigation

(def ^:private history (Html5History.))

(def ^:dynamic *title-suffix* "Acha-acha")

(defn set-title! [title]
  (set! (.-title js/document) (if title (str title " — " *title-suffix*) *title-suffix*)))

(defn go! [path]
  (.setToken history path))


;; Routes

(defroute index-route "/" []
  (set-title! nil)
  (r/render (index-page) (.-body js/document)))

(defroute user-route "/users/:id" [id]
  (let [id   (js/parseInt id)
        user (get @users id)]
    (set-title! (:name user))
    (r/render (user-page id) (.-body js/document))))

;; Rendering

(r/defc header []
  [:#header
    [:h1 {:on-click (fn [_] (go! "")) :style {:cursor "pointer"}} "Acha-acha"]
    [:h2 "Enterprise Git Achievement provider. Web scale. In the cloud "]])

(def repos (atom {}))

(defn repo-name [repo]
  (when-let [url (:url repo)]
    (let [[_ match] (re-matches #".*:(.*)" url)]
      match)))

(r/defc repo-pane []
  [:.repo_pane
    [:h1 "Repositories"]
    [:ul
      (map (fn [[_ r]]
             [:li (repo-name r)])
           @repos)]])

;; (def silhouette "https%3A%2F%2Fdl.dropboxusercontent.com%2Fu%2F561580%2Fsilhouette.png")

(r/defc user [user]
  (let [email-hash (when-let [email (:email user)] (js/md5 email))]
    [:.user {:on-click (fn [_] (go! (user-route {:id (:id user)}))) }
      [:.user__avatar
        [:img {:src (str "http://www.gravatar.com/avatar/" email-hash "?d=retro")}]]
      [:.user__name (:name user) [:span.user__id (:id user)]]
      [:.user__email (:email user)]
      [:.user__ach (:achievements user 0)]]))

(r/defc users-pane [users]
  [:.users_pane
    [:h1 "Users"]
    [:ul
      (map (fn [u] [:li (user u)]) users)]])

(r/defc index-page []
  [:#window
    (header)
    (repo-pane)
    (users-pane (vals @users))
   ])

(r/defc user-page [id]
  [:#window
    (header)
    (users-pane [(@users id)])
   ])

(defn redraw []
  (secretary/dispatch! (.getToken history)))

(defn ^:export start []
  (doto history
    (events/listen EventType/NAVIGATE (fn [e] (secretary/dispatch! (.-token e))))
    (.setUseFragment true)
    (.setPathPrefix "#")
    (.setEnabled true))
  (ajax "/api/users/" (fn [us]
                        (reset! users (map-by :id us))
                        (redraw)))
  (ajax "/api/repos/" (fn [us]
                        (reset! repos (map-by :id us))
                        (println @repos)
                        (redraw))))
