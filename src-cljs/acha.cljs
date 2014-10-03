(ns acha
  (:require
    [acha.react :as r :include-macros true]
    [sablono.core :as s :include-macros true]
    [goog.events :as events]
    [goog.history.EventType :as EventType]
    [datascript :as d]
    [acha.util :as u]
    [clojure.string :as str]
    [cognitect.transit :as transit]
    [cljs.core.async :as async]
    [clojure.set])
  (:import
    goog.history.Html5History
    goog.net.XhrIo)
  (:require-macros
    [acha :refer [profile]]
    [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)


;; DB

(def conn (d/create-conn {
  :ach/repo   {:db/valueType :db.type/ref}
  :ach/user   {:db/valueType :db.type/ref}
  :ach/achent {:db/valueType :db.type/ref}
}))

;; Utils

(defn read-transit [s]
  (transit/read (transit/reader :json) s))

(defn- ajax [url callback & [method]]
  (.send goog.net.XhrIo url
    (fn [reply]
      (let [res (.getResponseText (.-target reply))
            res (profile (str "read-transit " url " (" (count res) " bytes)") (read-transit res))]
        (js/setTimeout #(callback res) 0)))
    (or method "GET")))

(defn user-link [user]
  (str "/user/" (:user/email user)))

(defn repo-link [repo]
  (str "/repo/" (:repo/url repo)))

(defn map-by [f xs]
  (reduce (fn [acc x] (assoc acc (f x) x)) {} xs))

(defn trimr [s suffix]
  (let [pos (- (count s) (count suffix))]
    (if (and (>= (count s) (count suffix))
             (= (subs s pos) suffix))
      (subs s 0 pos)
      s)))

(defn starts-with? [s prefix]
  (= prefix (subs s 0 (count prefix))))

(defn repo-name [url]
  (when url
    (let [[_ m] (re-matches #".*/([^/]+)/?" url)]
      (trimr m ".git"))))

;; Navigation

(def ^:private history (Html5History.))

(def ^:dynamic *title-suffix* "Acha-acha")

(defn set-title! [title]
  (set! (.-title js/document) (if title (str title " — " *title-suffix*) *title-suffix*)))

(defn go! [& path]
  (.setToken history (apply str path)))


;; Rendering

(r/defc header [index?]
  (s/html
    [:.header
      [:a.vote {:href "https://clojurecup.com/?#/apps/acha"
                :target "_blank"}]
      (conj
        (if index?
          [:div.logo {:title "Acha-acha"}]
          [:a.logo   {:title "Acha-acha"
                      :href  "#" }])
        [:h2 "Enterprise Git Achievement solution" [:br] "Web scale. In the cloud"])]))

(r/defc footer []
  (s/html
    [:.footer
      [:.footer__copy   "© Copyright 2014 " [:a {:href "https://clojurecup.com/?#/apps/acha"} "Team Acha-acha"]]
      [:.footer__author [:img {:src "aches/wow@6x.png"}] "Nikita Prokopov"]
      [:.footer__author [:img {:src "aches/programmers-day@6x.png"}] "Andrey Vasenin"]
      [:.footer__author [:img {:src "aches/owl@6x.png"}] "Renat Idrisov"]
      [:.footer__author [:img {:src "aches/haskell@6x.png"}] "Dmitry Ivanov"]]))

;; (def silhouette "https%3A%2F%2Fdl.dropboxusercontent.com%2Fu%2F561580%2Fsilhouette.png")

(defn avatar [email & [w]]
  (when email
    (str "http://www.gravatar.com/avatar/" (js/md5 email) "?&d=retro" (when w (str "&s=" w)))))

(defn achent-img [achent]
  (when achent
    (str "aches/" (name (:achent/key achent)) "@6x.png")))


(r/defc repo [repo]
  (s/html
    [:a.repo {:key (:db/id repo)
              :href (str "#" (repo-link repo))}
      [:.repo__name
        (repo-name (:repo/url repo))
        [:.id (:db/id repo)]
        (let [status (:repo/status repo)
              class  (str "tag repo__status repo__status_" (name status))]
          [:span {:class class
                  :title (:repo/reason repo (name status))} (name status)])
       ]
      [:.repo__url (:repo/url repo)]     
      ]))

(defn add-repo []
  (let [el (.getElementById js/document "add_repo__input")]
    (doseq [url (str/split (.-value el) #"\s")
            :let [url (str/trim url)]
            :when (not (str/blank? url))]
      (println "Adding" url)
      (ajax (str "/api/add-repo/?url=" (js/encodeURIComponent url)) (fn [data]) "POST"))
    (set! (.-value el) "")
    (.focus el)))

(defn- sha1-url [url sha1]
  (let [path (condp re-matches url
               #"(?i)(?:https?://)?(?:www\.)?github.com/(.+)" :>> second
               #"(?i)git\@github\.com\:(.+)" :>> second
               nil)]
    (when path
      (str "https://github.com/" (trimr path ".git") "/commit/" sha1))))

(defn ach-details [ach]
  (str (get-in ach [:ach/user :user/name])
  " <" (get-in ach [:ach/user :user/email]) ">"
  "\n" (:ach/ts ach)
  "\n" (:ach/sha1 ach)
  "\n" (get-in ach [:ach/repo :repo/url])))

(r/defc ach-link [ach]
  (let [sha1     (:ach/sha1 ach)
        repo-url (get-in ach [:ach/repo :repo/url])
        text     (str (repo-name repo-url) "/" (subs sha1 0 7))
        title    (ach-details ach)]
    (s/html
      [:div {:key (:db/id ach)}
        (if-let [commit-url (sha1-url repo-url sha1)]
          [:a.ach__text { :target "_blank"
                          :href   commit-url
                          :title  title }
           text]
          [:.ach__text {:title title} text])])))

(r/defc last-ach [ach]
  (let [achent (:ach/achent ach)
        user   (:ach/user   ach)]
    (s/html
      [:.lach {:key (:db/id ach)}
        [:a.lach__user {:href (str "#" (user-link user)) }
          [:img.lach__user__img {:src (avatar (:user/email user) 114)}]
          [:.lach__user__name (:user/name user)]
          [:.lach__user__email (:user/email user)]]
        [:.lach__ach
          [:img.lach__ach__img {:src (achent-img achent)}]
          [:.lach__ach__name (:achent/name achent)]
          [:.lach__ach__desc (:achent/desc achent)]
          [:.lach__ach__links
            (ach-link ach)]
          ]])))
    

(r/defc repo-pane [db]
  (let [repos      (u/qes-by db :repo/url)
        last-aches (->> (d/datoms db :avet :ach/assigned) (take-last 10) reverse)]
    (s/html
      [:.repo_pane.pane
        [:h1 "Repos"]
        [:ul
          (map (fn [r] [:li {:key (:db/id r)} (repo r)]) repos)]
        [:form.add_repo {:on-submit (fn [e] (add-repo) (.preventDefault e))}
          [:input {:id "add_repo__input" :type :text :placeholder "Clone URL"}]
         ]
        [:h1 {:style {:margin-top 100 :margin-bottom 40}} "Last 10 achievements"]
        [:.laches
          (for [ach last-aches]
            (last-ach (d/entity db (.-e ach))))]
    ])))

(r/defc repo-profile [repo aches]
  (s/html
    [:.rp.pane
      [:.rp__name  (repo-name (:repo/url repo)) [:.id (:db/id repo)]]
      [:.rp__email (:repo/url repo)]
      [:.rp__hr]
      [:.rp__achs
        (for [[achent _] aches]
          [:img.rp__ach {:key   (:db/id achent)
                         :src   (achent-img achent)
                         :title (str (:achent/name achent) ":\n\n" (:achent/desc achent)) }])]]))



(r/defc user [user ach-cnt]
  (s/html
    [:a.user {:key (:db/id user)
              :href (str "#" (user-link user))}
      [:.user__avatar
        [:img {:src (avatar (:user/email user) 114)}]]
      [:.user__name (:user/name user) [:.id (:db/id user)]]
      [:.user__email (:user/email user)]
      (when ach-cnt [:.user__ach ach-cnt])]))

(r/defc user-profile [user aches]
  (s/html
    [:.up.pane
      [:.up__avatar
        [:img {:src (avatar (:user/email user) 228)}]]
      [:.up__name (:user/name user) [:.id (:db/id user)]]
      [:.up__email (:user/email user)]
      [:.up__hr]
      [:.up__achs
      (for [[achent _] aches]
        [:img.up__ach {:key   (:db/id achent)
                       :src   (achent-img achent)
                       :title (str (:achent/name achent) ":\n\n" (:achent/desc achent)) }])]]))

(r/defc users-pane [db users]
  (let [ach-cnt (u/qmap '[:find  ?u (count ?a)
                          :where [?e :ach/achent ?a]
                                 [?e :ach/user ?u]] db)
        users (->> users (sort-by #(ach-cnt (:db/id %) -1)) reverse)]
    (s/html
      [:.users_pane.pane
        [:h1 "Users"]
        [:ul
          (map (fn [u] [:li {:key (:db/id u)} (user u (ach-cnt (:db/id u)))]) users)]])))


(r/defc user-achent [achent aches]
  (s/html
    [:.ach { :key (:db/id achent) }
      [:.ach__logo
        [:img {:src (achent-img achent)}]]
      [:.ach__name (:achent/name achent)
        (let [max-lvl (reduce max 0 (map #(:ach/level % 0) aches))]
          (when (pos? max-lvl)
            [:.ach__level {:title (:achent/level-desc achent)} max-lvl]))
        ]
      [:.ach__desc (:achent/desc achent)]
      [:div.ach__links
        (for [ach aches]
          (ach-link ach))]]))

(r/defc repo-achent [achent aches]
  (s/html
    [:.ach { :key (:db/id achent) }
      [:.ach__logo
        [:img {:src (achent-img achent)}]]
      [:.ach__name (:achent/name achent)]
      [:.ach__desc (:achent/desc achent)]
      [:div.ach__users
        (for [ach aches
              :let [user   (:ach/user ach)
                    avatar (avatar (:user/email user) 114)]]
          [:a {:key  (:db/id user)
               :href (str "#" (user-link user))}
            [:img.ach__user {:src avatar
                             :title (ach-details ach)}]])]
     ]))

(r/defc ach-pane [aches component]
  (s/html
    [:.ach_pane.pane
      [:h1 "Achievements"]
      (map (fn [[achent aches]] (component achent aches)) aches)]))

(defn group-aches [aches]
  (->> aches
    (group-by :ach/achent)
    (sort-by (fn [[achent aches]] (list (reduce max 0 (map :ach/ts aches))
                                        (:achent/key achent))))
    reverse))

(r/defc index-page [db]
  (do
    (set-title! nil)
    (s/html
      [:div
        (users-pane db (u/qes-by db :user/email))
        (repo-pane  db)])))

(r/defc repo-page [db url]
  (let [repo  (u/qe-by db :repo/url url)
        aches (->> (:ach/_repo repo) group-aches)]
    (set-title! (repo-name url))
    (s/html
      [:div
        (ach-pane aches repo-achent)
        (repo-profile repo aches)])))

(r/defc user-page [db email]
  (let [user  (u/qe-by db :user/email email)
        aches (->> (:ach/_user user) group-aches)]
    (set-title! (:user/name user))
    (s/html
      [:div
        (ach-pane aches user-achent)
        (user-profile user aches)])))

(r/defc application [db]
  (let [path   (u/q1 '[:find ?p :where [0 :path ?p]] db)
        index? (str/blank? path)]
    (s/html
      [:.window
        (header index?)
        (cond
          index? (index-page db)
          (starts-with? path "/user/") (user-page db (subs path (count "/user/")))
          (starts-with? path "/repo/") (repo-page db (subs path (count "/repo/"))))
        (footer)])))


;; Progress bar

(r/defc progress-bar [progress]
  (s/html
    [:.progress
      [:.progress__bar {:style {:width (* progress 900)}}]]))

(defn render-progress [x]
  (r/render (progress-bar x) (.-body js/document)))

;; Rendering

(def render-db (atom nil))

(defn request-render [db]
  (reset! render-db db))

(defn render []
  (when-let [db @render-db]
    (r/render (application db) (.-body js/document))
    (reset! render-db nil)))

(add-watch render-db :render (fn [_ _ old-val new-val]
  (when (and (nil? old-val) new-val)
    (js/requestAnimationFrame render))))

;; Start

(defn ^:export start []
  (render-progress 0.1)
  (doto history
    (events/listen EventType/NAVIGATE (fn [e] (d/transact! conn [[:db/add 0 :path (.-token e)]])))
    (.setUseFragment true)
    (.setPathPrefix "#")
    (.setEnabled true))
  
  (let [socket (js/WebSocket. (str "ws://" (.. js/window -location -host) "/events"))]
    (set! (.-onmessage socket) (fn [event]
      (let [tx-data (read-transit (.-data event))]
        (profile (str "Pushed " (count tx-data) " entities")
          (d/transact! conn tx-data)))))
    (set! (.-onclose socket) (fn [event]
      (js/console.log event)))
    (set! (.-onerror socket) (fn [event]
      (js/console.log event))))
  
  (ajax "/api/db/"
    (fn [entities]
      (render-progress 0.2)
      (js/console.time "transact db")
      (let [progress (u/transact-async! conn entities
                       (fn []
                         (js/console.timeEnd "transact db")
                           (d/listen! conn
                             (fn [tx-report]
                               (request-render (:db-after tx-report))))
                           (println "Loaded db," (count (:eavt @conn)) "datoms")
                           (request-render @conn)))]
        (add-watch progress :progress (fn [_ _ _ new-val]
          (render-progress (/ (+ 0.3 new-val) 1.3))))))))



