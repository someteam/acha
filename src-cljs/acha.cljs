(ns acha
  (:require
    [acha.dom :as dom]
    [acha.util :as u]
    [acha.websocket :as ws]
    [cljs.core.async :as async]
    [clojure.set]
    [clojure.string :as str]
    [datascript :as d]
    [goog.events :as events]
    [goog.style]
    [goog.dom]
    [goog.events.EventType :as events.EventType]
    [rum]
    [sablono.core :as s])
  (:import
    goog.net.XhrIo)
  (:require-macros
    [acha :refer [profile]]
    [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

;; DB

(def ^:private schema {
  :ach/repo   {:db/valueType :db.type/ref}
  :ach/user   {:db/valueType :db.type/ref}
  :ach/achent {:db/valueType :db.type/ref}
})

(def conn (d/create-conn schema))

(def delta-chan (async/chan 100))

;; Utils

(defn user-link [user]
  (str "/user/" (:user/email user)))

(defn repo-link [repo]
  (str "/repo/" (:repo/url repo)))

(defn repo-name [url]
  (when url
    (if-let [[_ m] (re-matches #".*/([^/]+)/?" url)]
      (u/trimr m ".git")
      url)))

;; Rendering

(def app-state (atom {:progress    0
                      :users {:visible 0, :total 0}
                      :first-load? true
                      :path        "/"}))

(defn- index? [path]
  (or (= "/" path) (str/blank? path)))

(rum/defc progress-bar < rum/static [progress]
  (if (neg? progress)
    [:.progress.progress__offline]
    [:.progress
      [:.progress__bar
       {:style {:width (* progress 900)}}]]))

(rum/defc header < rum/static [state]
  (if (:first-load? state)
    [:.header (progress-bar (:progress state))])
    [:.header
      [:a {:href "https://github.com/someteam/acha"
           :target "_blank"}
        [:div.ribbon]]
      (conj
        (if (index? (:path state))
          [:div.logo {:title "Acha-acha"}]
          [:a.logo   {:title "Acha-acha"
                      :href  "#/" }])
        [:h2 "Enterprise Git Achievement Solution" [:br] "Web Scale. In the Cloud"])
      (progress-bar (:progress state))])

(rum/defc footer < rum/static [db]
  [:.footer
    [:.footer__copy   "© Copyright 2014 " [:a {:href "http://github.com/someteam"} "Some Team"]]
    [:a.footer__author {:href "https://github.com/tonsky"}      [:img {:src "aches/go@6x.png"}] "Nikita Prokopov"]
    [:a.footer__author {:href "https://github.com/avasenin"}    [:img {:src "aches/mark-of-the-beast@6x.png"}] "Andrey Vasenin"]
    [:a.footer__author {:href "https://github.com/parsifal-47"} [:img {:src "aches/owl@6x.png"}] "Renat Idrisov"]
    [:a.footer__author {:href "https://github.com/ethercrow"}   [:img {:src "aches/swift@6x.png"}] "Dmitry Ivanov"]
    [:a.footer__author {:href "https://vk.com/id59378819"}      [:img {:src "aches/wow@6x.png"}] "Julie Prokopova"]
    (let [meta (d/entity db 0)]
      [:.footer__version "App version " (:meta/app-version meta) ", db version " (:meta/db-version meta)])])

;; (def silhouette "https%3A%2F%2Fdl.dropboxusercontent.com%2Fu%2F561580%2Fsilhouette.png")

(defn avatar [email & [w]]
  (when email
    (str "//www.gravatar.com/avatar/" (js/md5 email) "?&d=retro" (when w (str "&s=" w)))))

(defn achent-img [achent]
  (when achent
    (str "aches/" (name (:achent/key achent)) "@6x.png")))

(rum/defc repo-status [repo]
  (let [status (:repo/status repo)
        class  (str "tag repo__status repo__status_" (name status))
        text   (str/capitalize (name status))]
    [:span (cond-> {:class class}
             (:repo/reason repo) (assoc :title (:repo/reason repo)))
      text]))

(rum/defc repo [repo]
  [:a.repo
     { :key (:db/id repo)
       :href (str "#" (repo-link repo)) }
    [:.repo__name
      (repo-name (:repo/url repo))
      [:.id (:db/id repo)]
      (repo-status repo)]
    [:.repo__url (:repo/url repo)]])

(defn add-repo []
  (let [el (.getElementById js/document "add_repo__input")]
    (doseq [url (str/split (.-value el) #"\s")
            :let [url (str/trim url)]
            :when (not (str/blank? url))]
      (println "Adding" url)
      (u/ajax (str "/api/add-repo/?url=" (js/encodeURIComponent url)) nil "POST"))
    (set! (.-value el) "")
    (.focus el)))

(defn delete-repo [id]
  (dom/go! "/")
  (u/ajax (str "/api/delete-repo/?id=" id) nil "POST"))

;; user@domain:path[.git]
;; http[s]://domain/path[.git]

(def ^:private repo-web-url
  (memoize (fn [clone-url]
    (condp re-matches clone-url
      #"([^@:]+)@([^@:]+):(.+)" :>> (fn [[_ user domain path]]
                                      (str "http://" domain "/"
                                           (-> path
                                             (u/trimr ".git")
                                             (u/trimr "/"))))
      #"(?i)(https?://.+)"      :>> (fn [[_ path]]
                                      (-> path
                                          (u/trimr ".git")
                                          (u/trimr "/")))
      nil))))

(defn- commit-web-url [clone-url sha1]
  (if clone-url
    (when-let [web-url (repo-web-url clone-url)]
      (condp re-find web-url
        #"(?i)^https?://(www\.)?bitbucket\.org" (str web-url "/commits/" sha1)
        (str web-url "/commit/" sha1)))
    nil)
  )

(defn ach-details [ach]
  (str (get-in ach [:ach/user :user/name])
  " <" (get-in ach [:ach/user :user/email]) ">"
  "\n" (:ach/ts ach)
  "\n" (:ach/sha1 ach)
  "\n" (get-in ach [:ach/repo :repo/url])))

(rum/defc ach-link < rum/static [ach]
  (let [sha1     (:ach/sha1 ach)
        repo-url (get-in ach [:ach/repo :repo/url])
        text     (str (repo-name repo-url) "/" (subs sha1 0 7))
        title    (ach-details ach)]
    [:div {:key (:db/id ach)}
      (if-let [commit-url (commit-web-url repo-url sha1)]
        [:a.ach__text { :target "_blank"
                        :href   commit-url
                        :title  title }
         text]
        [:.ach__text {:title title} text])]))

(rum/defc last-ach < rum/static [ach]
  (let [achent (:ach/achent ach)
        user   (:ach/user   ach)]
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
          (ach-link ach)]]]))

(rum/defc repo-pane < rum/static [db]
  (let [repos      (u/qes-by db :repo/url)
        last-aches (->> (d/datoms db :avet :ach/assigned) (take-last 10) reverse)]
    [:.repo_pane.pane
      [:h1 "Repos"]
      [:ul
        (map (fn [r] [:li {:key (:db/id r)} (repo r)]) repos)]
      [:form.add_repo {:on-submit (fn [e] (add-repo) (.preventDefault e))}
        [:input {:id "add_repo__input" :type :text :placeholder "Clone URL"}]
       ]
      (when (not-empty last-aches)
        (list
          [:h1 {:style {:margin-top 100 :margin-bottom 40}} "Last 10 achievements"]
          [:.laches
            (for [ach last-aches]
              (last-ach (d/entity db (.-e ach))))]))
    ]))

(rum/defc repo-profile < rum/static [repo aches]
  [:.rp.pane
    (let [repo-url (:repo/url repo)]
      (list
        [:.rp__name (repo-name repo-url)
          [:.id (:db/id repo)]
          (repo-status repo)]
        (if-let [web-url (repo-web-url repo-url)]
          [:a.rp__url {:href web-url :target "_blank"} repo-url]
          [:.rp__url repo-url])))
    (when-not (str/blank? (:repo/reason repo))
      (list
        [:.rp__reason (:repo/reason repo)]
        [:button.rp__delete {:on-click (fn [_] (delete-repo (:db/id repo)))}]))
    [:.rp__hr]
    [:.rp__achs
      (for [[achent _] aches]
        [:img.rp__ach {:key   (:db/id achent)
                       :src   (achent-img achent)
                       :title (str (:achent/name achent) ":\n\n" (:achent/desc achent)) }])]])



(rum/defc user < rum/static [user ach-cnt]
  [:a.user {:key (:db/id user)
            :href (str "#" (user-link user))}
    [:.user__avatar
      [:img {:src (avatar (:user/email user) 114)}]]
    [:.user__name (:user/name user) [:.id (:db/id user)]]
    [:.user__email (:user/email user)]
    (when ach-cnt [:.user__ach ach-cnt])])

(rum/defc user-profile < rum/static [user aches]
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
                     :title (str (:achent/name achent) ":\n\n" (:achent/desc achent)) }])]])
(defn- user-pane-appender [dom]
  (fn []
    (let [height        (+ (goog.style/getPageOffsetTop dom)
                           (.. (goog.style/getSize dom) -height))
          scroll-offset (.. (goog.dom/getDocumentScroll) -y)
          viewport      (.. (goog.dom/getViewportSize js/window) -height)]
      (when (<= height (+ scroll-offset viewport))
        ;; try to add new users
        (let [diff (- (get-in @app-state [:users :total])
                      (get-in @app-state [:users :visible]))]
          (when (pos? diff)
            (swap! app-state update-in [:users :visible] + (min 20 diff)))
        )))))

(def user-pane-appender-mixin {
  :did-mount (fn [state]
               (let [dom (.. (:rum/react-component state) getDOMNode)
                     listener (events/listen js/window events.EventType/SCROLL (user-pane-appender dom))]
                 (assoc state ::listener listener)))
  :transfer-state (fn [old-state new-state]
                    (merge new-state (select-keys old-state [::listener])))
  :did-update (fn [state]
                (let [dom (.. (:rum/react-component state) getDOMNode)]
                  ((user-pane-appender dom)))
                state)
  :will-unmount (fn [state]
                  (when-let [listener (::listener state)]
                    (events/unlistenByKey listener))
                  state)})

(rum/defc users-pane < rum/static user-pane-appender-mixin [db state users]
  (let [ach-cnt (u/qmap '[:find  ?u (count ?a)
                          :where [?e :ach/achent ?a]
                                 [?e :ach/user ?u]] db)
        visible (get-in state [:users :visible])
        users (->> users (sort-by #(ach-cnt (:db/id %) -1)) reverse (take visible))]
    [:.users_pane.pane
      [:h1 "Users"]
      (if (not-empty users)
        [:ul
          (map (fn [u] [:li {:key (:db/id u)} (user u (ach-cnt (:db/id u)))]) users)]
        [:.empty "Nobody achieved anything yet"])]))


(rum/defc user-achent < rum/static [achent aches]
  [:.ach { :key (:db/id achent) }
    [:.ach__logo
      [:img {:src (achent-img achent)}]]
    [:.ach__name (:achent/name achent)
      (let [max-lvl (reduce max 0 (map #(:ach/level % 0) aches))]
        (when (pos? max-lvl)
          [:.ach__level {:title (:achent/level-desc achent)} (str "lvl " max-lvl)]))
      ]
    [:.ach__desc (:achent/desc achent)]
    [:div.ach__links
      (for [ach aches]
        (ach-link ach))]])

(rum/defc repo-achent < rum/static [achent aches]
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
   ])

(rum/defc ach-pane < rum/static [aches component]
  [:.ach_pane.pane
    [:h1 "Achievements"]
    (if (not-empty aches)
      (map (fn [[achent aches]] (component achent aches)) aches)
      [:.empty "No achievements yet. Try harder!"])])

(defn group-aches [aches]
  (->> aches
    (group-by :ach/achent)
    (sort-by (fn [[achent aches]] (vector (reduce max 0 (map :ach/ts aches))
                                          (:achent/key achent))))
    reverse))

(rum/defc index-page < rum/static [db state]
  (do
    (dom/set-title! nil)
    [:div
      (users-pane db state (u/qes-by db :user/email))
      (repo-pane  db)]))

(rum/defc repo-page [db url]
  (let [repo  (u/qe-by db :repo/url url)
        aches (->> (:ach/_repo repo) group-aches)]
    (dom/set-title! (repo-name url))
    [:div
      (ach-pane aches repo-achent)
      (repo-profile repo aches)]))


(rum/defc user-page [db email]
  (let [user  (u/qe-by db :user/email email)
        aches (->> (:ach/_user user) group-aches)]
    (dom/set-title! (:user/name user))
    [:div
      (ach-pane aches user-achent)
      (user-profile user aches)]))

(def css-transition-group (.. js/React -addons -CSSTransitionGroup))

(rum/defc messages-overlay [db]
  (css-transition-group #js {:transitionName "popup" :className "messages"}
    (s/html
      (for [msg (u/qes-by db :message/text)]
        [:.message {:key (:db/id msg)
                    :class (name (:message/class msg))
                    :title "Click to close"
                    :on-click (fn [_] (d/transact! conn [[:db.fn/retractEntity (:db/id msg)]])) }
          (:message/text msg)]))))

(defn- inner-nav? [e]
  (when-let [a (dom/parent (.-target e) #(= (dom/node-name %) "a"))]
    (u/starts-with? (dom/attr a "href") "#")))

(rum/defc application < rum/cursored-watch [app-state conn]
  (let [db @conn
        state @app-state
        path (:path state)]
    [:.window {:on-click (fn [e] (when (inner-nav? e) (dom/scroll-to-top)))}
      (header state)
      (when-not (:first-load? state)
        (list
          (messages-overlay db)
          (cond
            (index? path) (index-page db state)
            (u/starts-with? path "/user/") (user-page db (subs path (count "/user/")))
            (u/starts-with? path "/repo/") (repo-page db (subs path (count "/repo/"))))
          (footer db)))]))

;; Start

(defn- listen-loop []
  (let [socket-ch (async/chan 1)
        data-ch   (async/chan 10)
        ajax-ch   (async/chan 1)
        socket (ws/connect "/events"
                 :on-open    #(async/put! socket-ch :open)
                 :on-close   #(doseq [ch [socket-ch ajax-ch data-ch]]
                                (async/close! ch))
                 :on-message #(async/put! data-ch %))]
    (go
      (when (async/<! socket-ch) ;; open socket
        (swap! app-state assoc :progress 0.3)
        (u/ajax "/api/db/" (fn [datoms] (async/put! ajax-ch datoms) (swap! app-state assoc :progress 0.6)))
        (let [[dump _] (async/alts! [ajax-ch (async/timeout 30000)])]
          (when dump  ;; wait ajax
            (profile "DB initialization"
              (reset! conn (d/init-db dump schema))
              (let [num-users (count (d/datoms @conn :aevt :user/name))
                    num-datoms (count (:eavt @conn))
                    num-achs (count (d/datoms @conn :aevt :ach/sha1))]
                (println "Pushed [datoms:" num-datoms "] [users:" num-users "] [achievements:" num-achs "]")
                (swap! app-state assoc
                  :users {:total num-users :visible (min 30 num-users)}
                  :first-load? false
                  :progress 1)))
            (loop []
              (when-let [tx-data (async/<! data-ch)]  ;; listen for socket
                (try
                  (d/transact! conn tx-data)
                  (let [num-users (count (d/datoms @conn :aevt :user/name))]
                    (swap! app-state assoc-in [:users :total] num-users))
                  (catch js/Error e
                    (.error js/console e)))
                (recur))))
          (.close socket)))
     (swap! app-state assoc :progress -1)
     (js/setTimeout listen-loop 1000))))

(defn ^:export start []
  (rum/mount (application app-state conn) (.-body js/document))
  (dom/listen-nav #(swap! app-state assoc :path %))

  (listen-loop)

  (d/listen! conn
    (fn [tx-report]
      (doseq [datom (:tx-data tx-report)
              :when (and (= (.-a datom) :message/text)
                         (.-added datom))]
        (js/setTimeout (fn []
                         (d/transact! conn [[:db.fn/retractEntity (.-e datom)]]))
                       10000)))))
