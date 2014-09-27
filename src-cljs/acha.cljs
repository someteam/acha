(ns acha
  (:require
    [acha.react :as r :include-macros true]
    [sablono.core :as s]))

(defn repos []
  [{:url "git@github.com:tonsky/datascript"}
   {:url "git@github.com:tonsky/41-socks"}
   {:url "git@github.com:tonsky/datascript-chat"}
   {:url "git@github.com:tonsky/net.async"}])

(defn repo-name [repo]
  (let [[_ match] (re-matches #".*:(.*)" (:url repo))]
    match))

(r/defc header []
  [:#header
    [:h1 "Acha-acha"]
    [:h2 "Enterprise Git Achievement provider. Web scale. In the cloud "]])

(r/defc repo-pane [repos]
  [:.repo_pane
    [:h1 "Repositories"]
    [:ul
      (map (fn [r] [:li (repo-name r)]) repos)]])

(r/defc window []
  [:#window
    (header)
    (repo-pane (repos))])

(defn ^:export start []
  (r/render (window) (.-body js/document)))
