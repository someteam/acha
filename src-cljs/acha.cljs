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
    [cljs.core.async.macros :refer [go go-loop]]))




(enable-console-print!)


;; DB

(def conn (d/create-conn {
  :ach/repo   {:db/valueType :db.type/ref}
  :ach/user   {:db/valueType :db.type/ref}
  :ach/achent {:db/valueType :db.type/ref}
}))

;; Utils

(defn- ajax [url callback & [method]]
  (.send goog.net.XhrIo url
    (fn [reply]
      (-> (.-target reply)
          (.getResponseText)
;;           (->> (.parse js/JSON))
;;           (js->clj :keywordize-keys true)
          (->> (transit/read (transit/reader :json)))
          (callback)))
    (or method "GET")))

(defn map-by [f xs]
  (reduce (fn [acc x] (assoc acc (f x) x)) {} xs))

(defn repo-name [url]
  (let [[_ m] (re-matches #".*/([^/]+)" url)]
    (if (and m (re-matches #".*\.git" m))
      (subs m 0 (- (count m) 4))
      m)))

;; Navigation

(def ^:private history (Html5History.))

(def ^:dynamic *title-suffix* "Acha-acha")

(defn set-title! [title]
  (set! (.-title js/document) (if title (str title " — " *title-suffix*) *title-suffix*)))

(defn go! [& path]
  (.setToken history (apply str path)))


;; Rendering

(r/defc header []
  (s/html
    [:.header
      [:h1.a {:on-click (fn [_] (go! "")) } "Acha-acha"]
      [:h2 "Enterprise Git Achievement solution. Web scale. In the cloud"]]))

(r/defc repo [repo]
  (s/html
    [:.repo.a {:on-click (fn [_] (go! "/repos/" (:repo/id repo)))}
      [:.repo__name
        (:repo/name repo)
        [:span.id (:repo/id repo)]
        (when (= :added (:repo/status repo)) [:span {:className "tag repo__added"} "Added"])]
      [:.repo__url (:repo/url repo)]     
      ]))

(defn add-repo []
  (let [el  (.getElementById js/document "add_repo__input")
        url (str/trim (.-value el))]
    (when-not (str/blank? url)
      (ajax (str "/api/add-repo/?url=" (js/encodeURIComponent url))
        (fn [data]
          (if (= :added (:repo/status data))
            (d/transact! conn [{:repo/id     (get-in data [:repo :id])
                                :repo/url    (get-in data [:repo :url])
                                :repo/name   (repo-name (get-in data [:repo :url]))
                                :repo/status :added}])
            (println "Repo already exist" data)))
        "POST")
      (set! (.-value el) "")
      (.focus el))))

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

(r/defc user [user]
  (let [email-hash (when-let [email (:user/email user)] (js/md5 email))]
    (s/html
      [:.user.a {:on-click (fn [_] (go! "/users/" (:user/id user)))}
        [:.user__avatar
          [:img {:src (str "http://www.gravatar.com/avatar/" email-hash "?d=retro")}]]
        [:.user__name (:user/name user) [:span.id (:user/id user)]]
        [:.user__email (:user/email user)]
        [:.user__ach (:user/ach user)]])))

(r/defc users-pane [users]
  (s/html
    [:.users_pane.pane
      [:h1 "Users"]
      [:ul
        (map (fn [u] [:li (user u)]) users)]]))

(r/defc ach [ach]
  (let [achent (:ach/achent ach)]
    (s/html
      [:.ach
        [:.ach__logo
          [:img {:src (str "aches/" (name (:achent/id achent)) "@6x.png")}]]
        [:.ach__name (:achent/name achent)
          (when-let [lvl (:ach/level ach 0)]
            [:.ach__level lvl])
          [:span.id (:ach/id ach)]]
        [:.ach__desc (:achent/desc achent)]
        [:.ach__repo (get-in ach [:ach/repo :repo/url])]])))

(r/defc ach-pane [aches]
  (s/html
    [:.ach_pane.pane
      [:h1 "Achievements"]
      [:ul
        (map (fn [a] [:li (ach a)]) aches)]]))

(r/defc index-page [db]
  (do
    (set-title! nil)
    (s/html
      [:.window
        (header)
        (users-pane (->> (u/qes-by db :user/id) (sort-by :user/ach) reverse))
        (repo-pane  (u/qes-by db :repo/name))])))

(r/defc repo-page [db id]
  (let [repo (u/qe-by db :repo/id id)]
    (set-title! (:repo/name repo))
    (s/html
      [:.window
        (header)
        (repo-pane [repo])])))

(r/defc user-page [db id]
  (let [user  (u/qe-by  db :user/id id)
        aches (->> (u/qes-by db :ach/user (:db/id user))
                   (sort-by :ach/ts)
                   reverse)]
    (set-title! (:user/name user))
    (s/html
      [:.window
        (header)
        (users-pane [user])
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
        (d/transact! conn
          (map (fn [u] {:user/id    (:id u)
                        :user/name  (:name u)
                        :user/email (:email u)
                        :user/ach   (:achievements u)})
                 us))
        (println "Loaded :users")
        (async/put! ch :users)))

    (ajax "/api/repos/"
      (fn [rs]
        (d/transact! conn
          (map (fn [r] {:repo/id   (:id r)
                        :repo/url  (:url r)
                        :repo/name (repo-name (:url r)) })
               rs))
        (println "Loaded :repos")
        (async/put! ch :repos)))

    (ajax "/api/ach-dir/"
      (fn [as]
        (d/transact! conn
          (map (fn [[k a]]
                 { :achent/id k
                   :achent/name (:name a)
                   :achent/desc (:description a) })
               as))
        (println "Loaded :achent")
        (async/put! ch :achent)))
  
    (go-loop [i 3]
      (if (pos? i)
        (do (<! ch) (recur (dec i)))

        (ajax "/api/ach/"
          (fn [as]

              
            (let [db @conn
                  aches (u/qmap '[:find ?id ?eid :where [?eid :achent/id ?id]] db)
                  users (u/qmap '[:find ?id ?eid :where [?eid :user/id ?id]]    db)
                  repos (u/qmap '[:find ?id ?eid :where [?eid :repo/id ?id]]    db)]
              
            (let [real (set (map keyword (map :type as)))
                  dict (set (keys aches))]
              
              (println "Missing" (clojure.set/difference real dict))
              (println "Shared"  (clojure.set/intersection dict real)))
              
              (d/transact! conn
                (->> as
                  (map (fn [a]
                         (when-let [achent (aches (keyword (:type a)))]
;;                            (println achent (:userid a) (:type a))
                           (cond->
                             { :ach/id     (:id a)
                               :ach/repo   (repos (:repoid a))
                               :ach/user   (users (:userid a))
                               :ach/achent achent
                               :ach/ts     (.parse js/Date (:timestamp a)) }
                             (:level a)
                               (assoc :ach/level (:level a))))))
                  (remove nil?)
                  check-tx)))
            (println "Loaded :ach")))
        )))
  )

