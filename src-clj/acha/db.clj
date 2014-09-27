(ns acha.db
  (:require [clojure.java.jdbc :refer :all]))

(def db
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
                       (create-table-ddl :repo
                                         [:id "integer primary key autoincrement"]
                                         [:url :text]
                                         [:state :text]
                                         [:reason :text]
                                         [:timestamp :text])
                       (create-table-ddl :achievement
                                         [:id "integer primary key autoincrement"]
                                         [:type :text]
                                         [:level :integer]
                                         [:userid :integer]
                                         [:repoid :integer]))
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
