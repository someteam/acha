(ns acha.db
  (:require [clojure.java.jdbc :refer :all]
            [acha.util :as util]))

(def ^:dynamic db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "acha-sqlite.db"
   })

(defn create-db []
  (try (db-do-commands db
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
                                         [:reason :text]
                                         [:timestamp :text])
                       "CREATE UNIQUE INDEX `url_unique` ON `repo` (`url` ASC)"
                       (create-table-ddl :achievement
                                         [:id "integer primary key autoincrement"]
                                         [:type :text]
                                         [:timestamp :text]
                                         [:level :integer]
                                         [:userid :integer]
                                         [:repoid :integer])
                       "CREATE INDEX `userid_index` ON `achievement` (`userid` ASC)"
                       "CREATE INDEX `repoid_index` ON `achievement` (`repoid` ASC)")
       (catch Exception e (println e))))

(defn add-fake-data []
  (insert! db :repo {:url "git@github.com:tonsky/datascript"})
  (insert! db :repo {:url "git@github.com:tonsky/41-socks"})
  (insert! db :repo {:url "git@github.com:tonsky/datascript-chat"})
  (insert! db :repo {:url "git@github.com:tonsky/net.async"})
  (insert! db :user {:name "Anders Hovmöller" :email "boxed@killingar.net"})
  (insert! db :user {:name "Bobby Calderwood" :email "bobby_calderwood@mac.com"})
  (insert! db :user {:name "Kevin J. Lynagh"  :email "kevin@keminglabs.com"})
  (insert! db :user {:name "Nikita Prokopov"  :email "prokopov@gmail.com"})
  (insert! db :user {:name "montyxcantsin"    :email "montyxcantsin@gmail.com"})
  (insert! db :user {:name "thegeez"          :email "thegeez@users.noreply.github.com"})
  )

(defn get-repo-list []
  (query db "SELECT r.*, count(a.id) AS achievements FROM repo r
    LEFT JOIN achievement a ON r.id=a.repoid GROUP BY r.id"))

(defn get-user-list []
  (query db "SELECT u.*, count(a.id) AS achievements FROM user u 
    LEFT JOIN achievement a ON u.id=a.userid GROUP BY u.id"))

(defn get-ach-list []
  (query db "SELECT * FROM achievement"))

(defn get-user-ach [id]
  (query db (str "SELECT * FROM achievement WHERE userid=" id)))

(defn get-repo-ach [id]
  (query db (str "SELECT * FROM achievement WHERE repoid=" id)))

(defn get-repo-by-url [url] 
  (first (query db ["select * from repo where url = ?" url])))

(defn get-or-insert-repo [url]
  (if-let [repo (get-repo-by-url url)]
    repo
    (do
      (insert! db :repo {:url url})
      (get-repo-by-url url))))

(defn get-user-by-email [email] 
  (first (query db ["select * from user where email = ?" email])))

(defn get-or-insert-user [email name]
  (let [email (util/normalize-str email)]
    (if-let [user (get-user-by-email email)]
      user
      (do
        (insert! db :user {:email email :name name})
        (get-user-by-email email)))))

(defn get-achievements-by-repo [id]
  (query db ["select achievement.*, user.* from achievement
             left join user on user.id = achievement.userid
             where repoid= ?" id]))

(defn insert-achievement [body]
  (insert! db :achievement body))

(defmacro with-connection [& body]
  `(with-db-connection [con# db]
      (binding [db con#]
        ~@body)))
