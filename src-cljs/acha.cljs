(ns acha
  (:require
    [acha.react :as r :include-macros true]
    [sablono.core :as s :include-macros true]
    [datascript :as d]
    [acha.dom :as dom]
    [acha.util :as u]
    [acha.websocket :as ws]
    [clojure.string :as str]
    [cljs.core.async :as async]
    [clojure.set])
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

(def state (atom {:progress    0
                  :first-load? true
                  :path        "/"}))

(r/defc progress-bar []
  (s/html
    (let [p (:progress @state)]
      (if (neg? p)
        [:.progress.progress__offline]
        [:.progress
          [:.progress__bar
           {:style {:width (* p 900)}}]]))))

(r/defc header [index?]
  (if (:first-load? @state)
    (s/html
      [:.header (progress-bar)])
    (s/html
      [:.header
        [:a {:href "https://github.com/someteam/acha"
             :target "_blank"}
          [:div.ribbon]]
        (conj
          (if index?
            [:div.logo {:title "Acha-acha"}]
            [:a.logo   {:title "Acha-acha"
                        :href  "#/" }])
          [:h2 "Enterprise Git Achievement Solution" [:br] "Web Scale. In the Cloud"])
        (progress-bar)])))

(r/defc footer []
  (s/html
    [:.footer
      [:.footer__copy   "© Copyright 2014 " [:a {:href "http://github.com/someteam"} "Some Team"]]
      [:a.footer__author {:href "https://github.com/tonsky"}      [:img {:src "aches/go@6x.png"}] "Nikita Prokopov"]
      [:a.footer__author {:href "https://github.com/avasenin"}    [:img {:src "aches/mark-of-the-beast@6x.png"}] "Andrey Vasenin"]
      [:a.footer__author {:href "https://github.com/parsifal-47"} [:img {:src "aches/owl@6x.png"}] "Renat Idrisov"]
      [:a.footer__author {:href "https://github.com/ethercrow"}   [:img {:src "aches/swift@6x.png"}] "Dmitry Ivanov"]
      [:a.footer__author {:href "https://vk.com/id59378819"}      [:img {:src "aches/wow@6x.png"}] "Julie Prokopova"]
      (let [meta (d/entity @conn 0)]
        [:.footer__version "App version " (:meta/app-version meta) ", db version " (:meta/db-version meta)])]))

;; (def silhouette "https%3A%2F%2Fdl.dropboxusercontent.com%2Fu%2F561580%2Fsilhouette.png")

(defn avatar [email & [w]]
  (when email
    (str "http://www.gravatar.com/avatar/" (js/md5 email) "?&d=retro" (when w (str "&s=" w)))))

(defn achent-img [achent]
  (when achent
    (str "aches/" (name (:achent/key achent)) "@6x.png")))

(r/defc repo-status [repo]
  (let [status (:repo/status repo)
        class  (str "tag repo__status repo__status_" (name status))
        text   (str/capitalize (name status))]
    (s/html
      [:span (cond-> {:class class}
               (:repo/reason repo) (assoc :title (:repo/reason repo)))
        text])))

(r/defc repo [repo]
  (s/html
    [:a.repo
       { :key (:db/id repo)
         :href (str "#" (repo-link repo)) }
      [:.repo__name
        (repo-name (:repo/url repo))
        [:.id (:db/id repo)]
        (repo-status repo)]
      [:.repo__url (:repo/url repo)]     
      ]))

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

(def ^:private repo-commit-prefix
  (memoize (fn [url]
    (condp re-matches url
      #"([^@:]+)@([^@:]+):(.+)" :>> (fn [[_ user domain path]]
                                      (str "http://" domain "/"
                                           (-> path
                                             (u/trimr ".git")
                                             (u/trimr "/"))
                                           "/commit/"))
      #"(?i)(https?://.+)"      :>> (fn [[_ path]]
                                      (-> path
                                          (u/trimr ".git")
                                          (u/trimr "/")
                                          (str "/commit/")))
      nil))))

(defn- sha1-url [url sha1]
  (when-let [prefix (repo-commit-prefix url)]
    (str prefix sha1)))

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
        (when (not-empty last-aches)
          (list
            [:h1 {:style {:margin-top 100 :margin-bottom 40}} "Last 10 achievements"]
            [:.laches
              (for [ach last-aches]
                (last-ach (d/entity db (.-e ach))))]))
    ])))

(r/defc repo-profile [repo aches]
  (s/html
    [:.rp.pane
      [:.rp__name (repo-name (:repo/url repo))
        [:.id (:db/id repo)]
        (repo-status repo)]
      [:.rp__url  (:repo/url repo)]
      (when-let [reason (:repo/reason repo)]
        (list 
          [:.rp__reason reason]
          [:button.rp__delete {:on-click (fn [_] (delete-repo (:db/id repo))) }]))
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
        (if (not-empty users)
          [:ul
            (map (fn [u] [:li {:key (:db/id u)} (user u (ach-cnt (:db/id u)))]) users)]
          [:.empty "Nobody achieved anything yet"]
          )])))


(r/defc user-achent [achent aches]
  (s/html
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
      (if (not-empty aches)
        (map (fn [[achent aches]] (component achent aches)) aches)
        [:.empty "No achievements yet. Try harder!"])]))

(defn group-aches [aches]
  (->> aches
    (group-by :ach/achent)
    (sort-by (fn [[achent aches]] (list (reduce max 0 (map :ach/ts aches))
                                        (:achent/key achent))))
    reverse))

(r/defc index-page [db]
  (do
    (dom/set-title! nil)
    (s/html
      [:div
        (users-pane db (u/qes-by db :user/email))
        (repo-pane  db)])))

(r/defc repo-page [db url]
  (let [repo  (u/qe-by db :repo/url url)
        aches (->> (:ach/_repo repo) group-aches)]
    (dom/set-title! (repo-name url))
    (s/html
      [:div
        (ach-pane aches repo-achent)
        (repo-profile repo aches)])))

(r/defc user-page [db email]
  (let [user  (u/qe-by db :user/email email)
        aches (->> (:ach/_user user) group-aches)]
    (dom/set-title! (:user/name user))
    (s/html
      [:div
        (ach-pane aches user-achent)
        (user-profile user aches)])))

(r/defc messages-overlay [db]
  ((-> js/React (aget "addons") (aget "CSSTransitionGroup"))
    #js {:transitionName "popup"
         :className "messages"}
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
       
(r/defc application [db]
  (let [path   (:path @state)
        index? (or (= "/" path) (str/blank? path))]
    (s/html
      [:.window {:on-click (fn [e] (when (inner-nav? e) (dom/scroll-to-top)))}
        (header index?)
        (when-not (:first-load? @state)
          (list
            (messages-overlay db)
            (cond
              index? (index-page db)
              (u/starts-with? path "/user/") (user-page db (subs path (count "/user/")))
              (u/starts-with? path "/repo/") (repo-page db (subs path (count "/repo/"))))
            (footer)))])))

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
        (swap! state assoc :progress 0.1)
        (u/ajax "/api/db/" (fn [tx-data] (async/put! ajax-ch tx-data)))
        (let [[dump _] (async/alts! [ajax-ch (async/timeout 30000)])]
          (when dump  ;; wait ajax
            (let [new-conn (d/create-conn schema)
                  parts    (partition-all 100 dump)
                  percent  (/ 0.89 (count parts))]
              (profile (str "Pushed " (count dump) " entities")
                (doseq [ents parts]
                  (d/transact! new-conn ents)
                  (swap! state update-in [:progress] + percent)
                  (async/<! (async/timeout 1)))) ;; temporary free js thread here
              (reset! conn @new-conn)
              (swap! state assoc
                :first-load? false
                :progress 1))
            (loop []
              (when-let [tx-data (async/<! data-ch)]  ;; listen for socket
                (try
                  (d/transact! conn tx-data)
                  (catch js/Error e
                    (.error js/console e)))
                (recur))))
          (.close socket)))
     (swap! state assoc :progress -1)
     (js/setTimeout listen-loop 1000))))

(defn ^:export start []
  (let [request-render (r/mount #(application @conn) (.-body js/document))]
    (add-watch state :rerender (fn [_ _ _ _] (request-render)))
    (add-watch conn  :rerender (fn [_ _ _ _] (request-render))))
  
  (dom/listen-nav #(swap! state assoc :path %))
  
  (listen-loop)
  
  (d/listen! conn
    (fn [tx-report]
      (doseq [datom (:tx-data tx-report)
              :when (and (= (.-a datom) :message/text)
                         (.-added datom))]
        (js/setTimeout (fn [] 
                         (d/transact! conn [[:db.fn/retractEntity (.-e datom)]]))
                       10000)))))
