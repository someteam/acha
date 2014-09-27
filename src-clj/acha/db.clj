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
  (insert! db :repo {:url "git@github.com:tonsky/net.async"}))

(defn get-repo-list []
  (query db "select * from repo"))

(defn get-user-list []
  (query db "select * from user"))

(defn get-ach-list []
  (query db "select * from achievement"))

(defn get-user-ach [id]
  (query db (str "select * from achievement where userid=" id)))

(defn get-repo-ach [id]
  (query db (str "select * from achievement where repoid=" id)))
