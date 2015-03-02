(ns acha.db
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.java.jdbc :refer :all]
    [clojure.tools.logging :as logging]
    [acha.core]
    [clojure.core.async :as async]
    [acha.util :as util]
    [acha.achievement-static :as static])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(def ^:const ^:private db-version "0.2.16")

(def ^:private gen-id-key (keyword "last_insert_rowid()"))

(defn db-path []
  (str acha.core/working-dir "/db.sqlite"))

(def pooled-db
  (delay
    (let [cpds (doto (ComboPooledDataSource.)
                 (.setDriverClass "org.sqlite.JDBC")
                 (.setJdbcUrl (str "jdbc:sqlite:" (db-path)))
                 (.setMinPoolSize 1)
                 (.setMaxPoolSize 1)
                 (.setInitialPoolSize 1)
                 ;; expire excess connections after 30 minutes of inactivity:
                 (.setMaxIdleTimeExcessConnections (* 30 60))
                 ;; expire connections after 3 hours of inactivity:
                 (.setMaxIdleTime (* 3 60 60)))] 
    {:datasource cpds})))

(defn db-conn [] @pooled-db)

(defn db-exists? []
  (let [f (io/as-file (db-path))]
    (and (.exists f) (pos? (.length f)))))

(defn drop-db []
  (try
    (when (db-exists?)
      (logging/info "Dropping DB" (db-path))
      (-> (db-conn) :datasource .hardReset)
      (io/delete-file (db-path)))
    (catch Exception e
      (logging/error e "Failed to drop DB"))))

(defn create-db []
  (when-not (db-exists?)
    (try
      (logging/info "Creating DB" (db-path))
      (db-do-commands (db-conn)
        (create-table-ddl :meta
                          [:version :text])
        (create-table-ddl :user
                          [:id :integer "primary key autoincrement"]
                          [:name :text]
                          [:email :text]
                          [:gravatar :text])
        "CREATE UNIQUE INDEX `email_unique` ON `user` (`email` ASC)"
        (create-table-ddl :repo
                          [:id :integer "primary key autoincrement"]
                          [:url :text]
                          [:state :text]
                          [:reason :text]
                          [:snapshot :integer]
                          [:timestamp :integer "not null default 0"])
        "CREATE UNIQUE INDEX `url_unique` ON `repo` (`url` ASC)"
        (create-table-ddl :scanned_commit
                          [:repoid :integer "references repo (id)"]
                          [:sha1 :text])
        (create-table-ddl :achievement
                          [:id :integer "primary key autoincrement"]
                          [:type :text]
                          [:timestamp :integer]
                          [:assigned  :integer]
                          [:level :integer]
                          [:userid :integer "references user (id)"]
                          [:repoid :integer "references repo (id)"]
                          [:sha1 :text])
        "CREATE INDEX `userid_index` ON `achievement` (`userid` ASC)"
        "CREATE INDEX `repoid_index` ON `achievement` (`repoid` ASC)")
      ;; non-overlapping ID ranges
      (insert! (db-conn) :sqlite_sequence {:seq 1000000 :name "user"})
      (insert! (db-conn) :sqlite_sequence {:seq 2000000 :name "repo"})
      (insert! (db-conn) :sqlite_sequence {:seq 3000000 :name "achievement"})
      (insert! (db-conn) :meta {:version db-version})
      (catch Exception e
        (logging/error e "Failed to initialize DB")))))

;; REPOS

(defn repo->entity [repo]
  (cond-> {:db/id       (:id repo)
           :repo/url    (:url repo)
           :repo/status (keyword (:state repo))}
    (:reason repo) (assoc :repo/reason (:reason repo))))

(defn get-repo-list []
  (query (db-conn) "SELECT r.* FROM repo r"))

(defn get-repo-by-url [url] 
  (first (query (db-conn) ["select * from repo where url = ?" url])))

(defn get-repo [id]
  (first (query (db-conn) ["select * from repo where id = ?" id])))

(defn get-or-insert-repo [url]
  (let [url (util/canonical-repo-url url)]
    (if-let [repo (get-repo-by-url url)]
      :exists
      (let [_    (insert! (db-conn) :repo {:url url :state "waiting"})
            repo (get-repo-by-url url)]
        (async/put! acha.core/events [(repo->entity repo)])
        repo))))

(defn update-repo-state
  ([id state]
    (update! (db-conn) :repo {:state (name state)
                              :reason nil}
             ["id = ?" id])
    (async/put! acha.core/events [[:db/add id :repo/status state]
                                  [:db.fn/retractAttribute id :repo/reason]]))
  ([id state reason]
    (update! (db-conn) :repo {:state (name state)
                              :reason reason}
             ["id = ?" id])
    (async/put! acha.core/events [{:db/id id
                                   :repo/status state
                                   :repo/reason reason}
                                  ])))

(defn get-next-repo []
  (first (query (db-conn) ["select * from repo where (timestamp < ?) and state in ('idle', 'waiting', 'error')
      order by timestamp asc limit 1" (- (quot (System/currentTimeMillis) 1000) (* 15 60))])))

(defn try-to-update [repo]
  (pos? (reduce + (update! (db-conn) :repo {:timestamp (quot (System/currentTimeMillis) 1000)}
    ["id = ? and timestamp = ?" (:id repo) (:timestamp repo)]))))

(defn get-next-repo-to-process []
  (when-let [repo (get-next-repo)]
    (if (try-to-update repo) repo [])))

(defn count-new-repos []
  (count (query (db-conn) "select * from repo where state = \"waiting\"")))

(defn get-scanned-commits [repo-id]
  (->> (query (db-conn) ["select * from scanned_commit where repoid = ?" repo-id])
       (map :sha1)
       set))

(defn insert-scanned-commit [repo-id sha1]
  (insert! (db-conn) :scanned_commit {:repoid repo-id :sha1 sha1}))

(defn update-scanned-timeline [repo-id snapshot]
  (update! (db-conn) :repo {:snapshot snapshot} ["id = ?" repo-id]))

(defn delete-repo [id]
  (let [conn  (db-conn)
        aches (query conn ["select id from achievement where (repoid = ?)" id])
        _     (delete! conn :achievement ["repoid = ?" id])
        users (query conn "select id from user where id not in (select distinct userid from achievement)")
        _     (delete! conn :user ["id not in (select distinct userid from achievement)"])
        _     (delete! conn :scanned_commit ["repoid = ?" id])
        _     (delete! conn :repo ["id = ?" id])]
    (concat 
      (map :id aches)
      (map :id users)
      [id])))


;; USERS

(defn user->entity [user]
  {:db/id      (:id user)
   :user/email (:email user)
   :user/name  (:name user)})

(defn get-user-list []
  (query (db-conn) "SELECT u.* FROM user u"))

(defn get-user-by-email [email]
  (first (query (db-conn) ["select * from user where email = ?" email])))

(defn insert-users [users]
  (let [db       (db-conn)
        inserted (for [user  users
                       :let  [email (util/normalize-str (:email user))]
                       :when (empty? (query db ["select * from user where email = ?" email]))]
                   (do
                     (insert! db :user {:email email :name (:name user)})
                     (get-user-by-email email)))]
    (async/put! acha.core/events (mapv user->entity inserted))))


;; ACHIEVEMENTS

(defn achent->entity [a]
  (cond->
    { :db/id       (:id a)
      :achent/key  (:key a)
      :achent/name (:name a)
      :achent/desc (:description a) }
   (:level-description a)
     (assoc :achent/level-desc (:level-description a))))

(defn ach->entity [a]
  (cond->
    { :db/id        (:id a)
      :ach/repo     (:repoid a)
      :ach/user     (:userid a)
      :ach/achent   (-> (:type a) keyword static/table-map :id)
      :ach/sha1     (:sha1 a)
      :ach/ts       (java.util.Date. (long (:timestamp a)))
      :ach/assigned (java.util.Date. (long (:assigned a)))}
    (:level a)
      (assoc :ach/level (:level a))))

(defn get-ach-list []
  (query (db-conn) "SELECT * FROM achievement"))

(defn get-achievements-by-repo [id]
  (query (db-conn) ["select achievement.*, user.* from achievement
             left join user on user.id = achievement.userid
             where repoid= ?" id]))

(defn insert-achievements [aches]
  (let [db (db-conn)
        entities (for [ach aches
                       :let [[db-res] (insert! db :achievement ach)]]
                   (ach->entity (assoc ach :id (gen-id-key db-res))))]
    (async/put! acha.core/events (vec entities))))

;; META

(defn db-meta []
  (first (query (db-conn) "SELECT * FROM meta")))

(defn initialize-db []
  (logging/info "Initialize db")
  (if (db-exists?)
    (when-not (= (:version (db-meta)) db-version)
      (let [repos (get-repo-list)]
        (drop-db)
        (create-db)
        (logging/info "Add repos" repos)
        (doseq [{url :url} repos] (get-or-insert-repo url))))
    (create-db))
  (-> (db-conn) :datasource .hardReset)
  (update! (db-conn) :repo {:timestamp 0} ["state <> ?" "idle"])
  (update! (db-conn) :repo {:state "idle" :reason nil} ["state <> ?" "error"]))


;; CLEANUP

(defn wipe-repo-timestamps! []
  (update! (db-conn) :repo {:state "waiting" :timestamp 0} []))

(defn wipe-aches! []
  (delete! (db-conn) :achievement [])
  (delete! (db-conn) :user [])
  (delete! (db-conn) :repo_seen [])
  (wipe-repo-timestamps!))

(defn wipe-all! []
  (wipe-aches!)
  (delete! (db-conn) :repo []))


;; (wipe-repo-timestamps!)
;; (wipe-aches!)
;; (wipe-all!)




