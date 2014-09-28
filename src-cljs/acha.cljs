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

(defn map-by [f xs]
  (reduce (fn [acc x] (assoc acc (f x) x)) {} xs))

(defn trimr [s suffix]
  (let [pos (- (count s) (count suffix))]
    (if (and (>= (count s) (count suffix))
             (= (subs s pos) suffix))
      (subs s 0 pos)
      s)))

(defn repo-name [url]
  (let [[_ m] (re-matches #".*/([^/]+)/?" url)]
    (trimr m ".git")))

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
      [:div.logo {:class    (when-not index? "a")
                  :title    "Acha-acha"
                  :on-click (fn [_] (go! "")) }
        [:h2 "Enterprise Git Achievement solution." [:br] "Web scale. In the cloud"]]]))

(r/defc repo [repo]
  (s/html
    [:.repo.a {:key (:db/id repo)
               :on-click (fn [_] (go! "/repos/" (:repo/id repo)))}
      [:.repo__name
        (:repo/name repo)
        [:.id (:repo/id repo)]
        (when (= :added (:repo/status repo)) [:span {:class "tag repo__added"} "Added"])]
      [:.repo__url (:repo/url repo)]     
      ]))

(defn add-repo []
  (let [el (.getElementById js/document "add_repo__input")]
    (doseq [url (str/split (.-value el) #"\s")
            :let [url (str/trim url)]
            :when (not (str/blank? url))]
      (println "Adding" url)
      (ajax (str "/api/add-repo/?url=" (js/encodeURIComponent url))
        (fn [data]
          (if (= :added (:repo/status data))
            (d/transact! conn [{:repo/id     (get-in data [:repo :id])
                                :repo/url    (get-in data [:repo :url])
                                :repo/name   (repo-name (get-in data [:repo :url]))
                                :repo/status :added}])
            (println "Repo already exist" data)))
        "POST"))
    (set! (.-value el) "")
    (.focus el)))

(r/defc repo-pane [repos]
  (s/html
    [:.repo_pane.pane
      [:h1 "Repos"]
      [:ul
        (map (fn [r] [:li (repo r)]) repos)]
      [:form.add_repo {:on-submit (fn [e] (add-repo) (.preventDefault e))}
        [:input {:id "add_repo__input" :type :text :placeholder "Clone URL"}]
;;         [:button]
       ]
    ]))

;; (def silhouette "https%3A%2F%2Fdl.dropboxusercontent.com%2Fu%2F561580%2Fsilhouette.png")

(r/defc user [user ach-cnt]
  (let [email-hash (when-let [email (:user/email user)] (js/md5 email))]
    (s/html
      [:.user.a {:key (:db/id user)
                 :on-click (fn [_] (go! "/users/" (:user/id user)))}
        [:.user__avatar
          [:img {:src (str "http://www.gravatar.com/avatar/" email-hash "?d=retro")}]]
        [:.user__name (:user/name user) [:.id (:user/id user)]]
        [:.user__email (:user/email user)]
        (when ach-cnt [:.user__ach ach-cnt])])))

(r/defc users-pane [db users]
  (let [ach-cnt (u/qmap '[:find  ?u (count ?a)
                          :where [?e :ach/achent ?a]
                                 [?e :ach/user ?u]] db)
        users (->> users (sort-by #(ach-cnt (:db/id %) -1)) reverse)]
    (s/html
      [:.users_pane.pane
        [:h1 "Users"]
        [:ul
          (map (fn [u] [:li (user u (ach-cnt (:db/id u)))]) users)]])))

(defn- sha1-url [url sha1]
  (let [path (condp re-matches url
               #"(?i)(?:https?://)?(?:www\.)?github.com/(.+)" :>> second
               #"(?i)git\@github\.com\:(.+)" :>> second
               nil)]
    (when path
      (str "https://github.com/" (trimr path ".git") "/commit/" sha1))))

(r/defc ach [achent aches]
  (s/html
    [:.ach { :key (:db/id achent) }
      [:.ach__logo
        [:img {:src (str "aches/" (name (:achent/id achent)) "@6x.png")}]]
      [:.ach__name (:achent/name achent)
        (let [max-lvl (reduce max 0 (map #(:ach/level % 0) aches))]
          (when (pos? max-lvl)
            [:.ach__level {:title (:achent/level-desc achent)} max-lvl]))
        ]
      [:.ach__desc (:achent/desc achent)]
      (for [ach  aches
            :let [sha1     (:ach/sha1 ach)
                  repo-url (get-in ach [:ach/repo :repo/url])
                  text     (str (subs sha1 0 7) " @ " (repo-name repo-url))]]
        [:div
          (if-let [commit-url (sha1-url repo-url sha1)]
            [:a.ach__link { :target "__blank"
                            :href  commit-url
                            :title sha1 }
             text]
            [:.ach__sha1 text])
         [:.id (:ach/id ach)]])
     ]))

(r/defc ach-pane [aches]
  (s/html
    [:.ach_pane.pane
      [:h1 "Achievements"]
      [:ul
        (->> aches
             (group-by :ach/achent)
             (map (fn [[achent aches]] [:li (ach achent aches)])))]]))

(r/defc index-page [db]
  (do
    (set-title! nil)
    (s/html
      [:.window
        (header true)
        (users-pane db (u/qes-by db :user/id))
        (repo-pane  (u/qes-by db :repo/name))])))

(r/defc repo-page [db id]
  (let [repo (u/qe-by db :repo/id id)]
    (set-title! (:repo/name repo))
    (s/html
      [:.window
        (header false)
        (repo-pane [repo])])))

(r/defc user-page [db id]
  (let [user  (u/qe-by  db :user/id id)
        aches (->> (u/qes-by db :ach/user (:db/id user))
                   (sort-by :ach/ts)
                   reverse)]
    (set-title! (:user/name user))
    (s/html
      [:.window
        (header false)
        (users-pane db [user])
        (ach-pane aches)])))

(r/defc application [db]
  (let [path      (u/q1 '[:find ?p :where [0 :path ?p]] db)
        [_ p0 p1] (str/split path #"/")]
    (cond
      (= p0 nil)     (index-page db)
      (= p0 "users") (user-page db (js/parseInt p1))
      (= p0 "repos") (repo-page db (js/parseInt p1)))))

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

(defn check-tx [es]
  (doseq [e es
          [k v] e]
    (if (nil? v)
      (println e)))
  es)

(defn ^:export start []
  (d/listen! conn
    (fn [tx-report]
      (request-render (:db-after tx-report))))

  (doto history
    (events/listen EventType/NAVIGATE (fn [e] (d/transact! conn [[:db/add 0 :path (.-token e)]])))
    (.setUseFragment true)
    (.setPathPrefix "#")
    (.setEnabled true))
  
  (let [ch (async/chan 3)]
    
    (ajax "/api/users/"
      (fn [us]
        (profile "transact :users"
          (d/transact! conn
            (map (fn [u] {:user/id    (:id u)
                          :user/name  (:name u)
                          :user/email (:email u)
                          :user/ach   (:achievements u)})
                   us)))
        (println "Loaded :users," (count (:eavt @conn)) "datoms")
        (async/put! ch :users)))

    (ajax "/api/repos/"
      (fn [rs]
        (profile "transact :repos"
          (d/transact! conn
            (map (fn [r] {:repo/id   (:id r)
                          :repo/url  (:url r)
                          :repo/name (repo-name (:url r)) })
                 rs)))
        (println "Loaded :repos," (count (:eavt @conn)) "datoms")
        (async/put! ch :repos)))

    (ajax "/api/ach-dir/"
      (fn [as]
        (profile "transact :achent"
          (d/transact! conn
            (map (fn [[k a]]
                   { :achent/id k
                     :achent/name (:name a)
                     :achent/desc (:description a)
                     :achent/level-desc (:level-description a)})
                 as)))
        (println "Loaded :achent," (count (:eavt @conn)) "datoms")
        (async/put! ch :achent)))
  
    (go-loop [i 3]
      (if (pos? i)
        (do (<! ch) (recur (dec i)))

        (ajax "/api/ach/"
          (fn [as]
            (let [db @conn
                  aches (u/qmap '[:find ?id ?eid :where [?eid :achent/id ?id]] db)
                  users (u/qmap '[:find ?id ?eid :where [?eid :user/id ?id]]   db)
                  repos (u/qmap '[:find ?id ?eid :where [?eid :repo/id ?id]]   db)]
              (profile "transact :ach"
                (d/transact! conn
                  (->> as
                    (map (fn [a]
                           (when-let [achent (aches (keyword (:type a)))]
                             (cond->
                               { :ach/id     (:id a)
                                 :ach/repo   (repos (:repoid a))
                                 :ach/user   (users (:userid a))
                                 :ach/achent achent
                                 :ach/sha1   (:sha1 a)
                                 :ach/ts     (.parse js/Date (:timestamp a)) }
                               (:level a)
                                 (assoc :ach/level (:level a))))))
                    (remove nil?)
                    check-tx))))
            (println "Loaded :ach," (count (:eavt @conn)) "datoms")))
        )))
  )

