(ns acha.db
  (:require
    [clojure.java.io :as io]
    [clojure.java.jdbc :refer :all]
    [clojure.tools.logging :as logging]
    [acha.util :as util])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(def db-spec
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "acha-sqlite.db"})

(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec)) 
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               (.setMinPoolSize 1)
               (.setMaxPoolSize 1)
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))] 
    {:datasource cpds}))

(def pooled-db (delay (pool db-spec)))
(defn db-conn [] @pooled-db)

(defn create-db []
  (when-not (.exists (io/as-file (:subname db-spec)))
    (try
      (logging/info "Creating DB" (:subname db-spec))
      (db-do-commands (db-conn)
        (create-table-ddl :user
                          [:id "integer primary key autoincrement"]
                          [:name :text]
                          [:email :text]
                          [:gravatar :text])
        "CREATE UNIQUE INDEX `email_unique` ON `user` (`email` ASC)"
        (create-table-ddl :repo
                          [:id "integer primary key autoincrement"]
                          [:url :text]
                          [:state :text]
                          [:sha1 :text]
                          [:reason :text]
                          [:timestamp "integer not null default 0"])
        "CREATE UNIQUE INDEX `url_unique` ON `repo` (`url` ASC)"
        (create-table-ddl :achievement
                          [:id "integer primary key autoincrement"]
                          [:type :text]
                          [:timestamp :text]
                          [:level :integer]
                          [:userid :integer]
                          [:repoid :integer]
                          [:sha1 :text])
        "CREATE INDEX `userid_index` ON `achievement` (`userid` ASC)"
        "CREATE INDEX `repoid_index` ON `achievement` (`repoid` ASC)")
      (catch Exception e
        (logging/error e "Failed to initialize DB")))))

(defn add-fake-data []
  (insert! (db-conn) :repo {:url "git@github.com:tonsky/datascript.git"})
  (insert! (db-conn) :repo {:url "git@github.com:tonsky/41-socks.git"})
  (insert! (db-conn) :repo {:url "git@github.com:tonsky/datascript-chat.git"})
  (insert! (db-conn) :repo {:url "git@github.com:tonsky/net.async.git"})
  (insert! (db-conn) :user {:name "Anders Hovmöller" :email "boxed@killingar.net"})
  (insert! (db-conn) :user {:name "Bobby Calderwood" :email "bobby_calderwood@mac.com"})
  (insert! (db-conn) :user {:name "Kevin J. Lynagh"  :email "kevin@keminglabs.com"})
  (insert! (db-conn) :user {:name "Nikita Prokopov"  :email "prokopov@gmail.com"})
  (insert! (db-conn) :user {:name "montyxcantsin"    :email "montyxcantsin@gmail.com"})
  (insert! (db-conn) :user {:name "thegeez"          :email "thegeez@users.noreply.github.com"})
  )

(defn get-repo-list []
  (query (db-conn) "SELECT r.* FROM repo r"))

(defn get-user-list []
  (query (db-conn) "SELECT u.* FROM user u"))

(defn get-ach-list []
  (query (db-conn) "SELECT * FROM achievement"))

(defn get-repo-by-url [url] 
  (first (query (db-conn) ["select * from repo where url = ?" url])))

(defn get-or-insert-repo [url]
  (let [url (util/normalize-str url)]
    (if-let [repo (get-repo-by-url url)]
      repo
      (do
        (insert! (db-conn) :repo {:url url :state "new"})
        (get-repo-by-url url)))))

(defn get-user-by-email [email] 
  (first (query (db-conn) ["select * from user where email = ?" email])))

(defn get-or-insert-user [email name]
  (let [email (util/normalize-str email)]
    (if-let [user (get-user-by-email email)]
      user
      (do
        (insert! (db-conn) :user {:email email :name name})
        (get-user-by-email email)))))

(defn get-achievements-by-repo [id]
  (query (db-conn) ["select achievement.*, user.* from achievement
             left join user on user.id = achievement.userid
             where repoid= ?" id]))

(defn get-next-repo []
  (first (query (db-conn) ["select * from repo where (timestamp < ?)
      order by timestamp asc limit 1" (- (quot (System/currentTimeMillis) 1000) (* 15 60))])))

(defn- try-to-update [repo]
  (pos? (update! (db-conn) :repo {:timestamp (quot (System/currentTimeMillis) 1000)}
    ["id = ? and timestamp = ?" (:id repo) (:timestamp repo)])))

(defn get-next-repo-to-process []
  (when-let [repo (get-next-repo)]
    (if (try-to-update repo) repo [])))

(defn insert-achievement [body]
  (insert! (db-conn) :achievement body))

(defn update-repo-sha1 [repo-id sha1]
  (update! (db-conn) :repo {:sha1 sha1 :state "ok"} ["id = ?" repo-id]))

(defn update-repo-state [repo-id state]
  (update! (db-conn) :repo {:state state} ["id = ?" repo-id]))

(defn count-new-repos []
  (count (query (db-conn) "select * from repo where state = \"new\"")))
