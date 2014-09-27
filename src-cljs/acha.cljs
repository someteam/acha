(ns acha
  (:require
    [acha.react :as r :include-macros true]
    [sablono.core :as s]
    [secretary.core :as secretary :include-macros true :refer [defroute]]
    [goog.events :as events]
    [goog.history.EventType :as EventType])
  (:import
    goog.history.Html5History))

(enable-console-print!)
(declare index-page user-page users)

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
  (let [user (first (users))]
    (set-title! (:name user)))
  (r/render (user-page id) (.-body js/document)))

;; Rendering

(defn repos []
  [{:url "git@github.com:tonsky/datascript"}
   {:url "git@github.com:tonsky/41-socks"}
   {:url "git@github.com:tonsky/datascript-chat"}
   {:url "git@github.com:tonsky/net.async"}])

(defn repo-name [repo]
  (let [[_ match] (re-matches #".*:(.*)" (:url repo))]
    match))

(defn users []
  [ {:name "Anders Hovmöller" :email "boxed@killingar.net"  :achievements 17}
    {:name "Bobby Calderwood" :email "bobby_calderwood@mac.com"  :achievements 99}
    {:name "Kevin J. Lynagh"  :email "kevin@keminglabs.com"  :achievements 25}
    {:name "Nikita Prokopov"  :email "prokopov@gmail.com"  :achievements 3}
    {:name "montyxcantsin"    :email "montyxcantsin@gmail.com"  :achievements 0}
    {:name "thegeez"          :email "thegeez@users.noreply.github.com"  :achievements 21} ])


(r/defc header []
  [:#header
    [:h1 {:on-click (fn [_] (go! "")) :style {:cursor "pointer"}} "Acha-acha"]
    [:h2 "Enterprise Git Achievement provider. Web scale. In the cloud "]])

(r/defc repo-pane []
  [:.repo_pane
    [:h1 "Repositories"]
    [:ul
      (map (fn [r] [:li (repo-name r)]) (repos))]])

;; (def silhouette "https%3A%2F%2Fdl.dropboxusercontent.com%2Fu%2F561580%2Fsilhouette.png")

(r/defc user [user]
  (let [email-hash (js/md5 (:email user))]
    [:.user {:on-click (fn [_] (go! (user-route {:id (:email user)}))) }
      [:.user__avatar
        [:img {:src (str "http://www.gravatar.com/avatar/" email-hash "?d=retro")}]]
      [:.user__name (:name user)]
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
    (users-pane (users))
   ])

(r/defc user-page [id]
  [:#window
    (header)
    (users-pane [(first (users))])
   ])

(defn ^:export start []
  (doto history
    (events/listen EventType/NAVIGATE (fn [e] (secretary/dispatch! (.-token e))))
    (.setUseFragment true)
    (.setPathPrefix "#")
    (.setEnabled true)))
