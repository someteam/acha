(ns acha.db
  (:require [clojure.java.jdbc :refer :all]))

(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "resources/acha.db"
   })

(defn create-db []
  (try (db-do-commands db
                       (create-table-ddl :users
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

(create-db)

;(def testdata
;  {:name "test",
;   :email "t@email.com"})

;(insert! db :users testdata)

(defn get-all-users []
  (query db "select * from users"))

;(keys (first output))
;(:body (first output))