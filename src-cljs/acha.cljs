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

(defn users []
  [ {:name "Anders Hovmöller" :email "boxed@killingar.net"  :achievements 17}
    {:name "Bobby Calderwood" :email "bobby_calderwood@mac.com"  :achievements 99}
    {:name "Kevin J. Lynagh"  :email "kevin@keminglabs.com"  :achievements 25}
    {:name "Nikita Prokopov"  :email "prokopov@gmail.com"  :achievements 3}
    {:name "montyxcantsin"    :email "montyxcantsin@gmail.com"  :achievements 0}
    {:name "thegeez"          :email "thegeez@users.noreply.github.com"  :achievements 21} ])


(r/defc header []
  [:#header
    [:h1 "Acha-acha"]
    [:h2 "Enterprise Git Achievement provider. Web scale. In the cloud "]])

(r/defc repo-pane []
  [:.repo_pane
    [:h1 "Repositories"]
    [:ul
      (map (fn [r] [:li (repo-name r)]) (repos))]])

(r/defc user [user]
  (let [email-hash (js/md5 (:email user))]
  [:.user
    [:.user__avatar
      [:img {:src (str "http://www.gravatar.com/avatar/" email-hash)}]]
    [:.user__name (:name user)]
    [:.user__email (:email user)]
    [:.user__ach (:achievements user 0)]]))

(r/defc users-pane []
  [:.users_pane
    [:h1 "Users"]
    [:ul
      (map (fn [u] [:li (user u)]) (users))]])

(r/defc window []
  [:#window
    (header)
    (repo-pane)
    (users-pane)
   ])

(defn ^:export start []
  (r/render (window) (.-body js/document)))
