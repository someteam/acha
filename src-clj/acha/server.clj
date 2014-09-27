(ns acha.server
  (:require
    [acha.git-parser :as git-parser]
    [acha.achievement :as achievement]
    [acha.db :as db]
    [clojure.tools.logging :as logging]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.reload :as reload]
    [ring.util.response :as response]
    [clj-json.core :as json]
    [compojure core route])
  (:gen-class))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

(def handler (compojure.core/routes
               (compojure.route/resources "/")
               (compojure.core/GET "/" []
                 (response/content-type (response/resource-response "public/index.html") "text/html"))
               (compojure.core/GET "/api/repos/" []
                 (json-response (db/get-repo-list)))
               (compojure.core/GET ["/api/repos/:id", :id #"[0-9]+"] [id]
                 (json-response (db/get-repo-ach id)))
               (compojure.core/GET "/api/users/" []
                 (json-response (db/get-user-list)))
               (compojure.core/GET ["/api/users/:id", :id #"[0-9]+"] [id]
                 (json-response (db/get-user-ach id)))
               (compojure.core/GET "/api/ach/" []
                 (json-response (db/get-ach-list)))
               (compojure.route/not-found "Page not found")))
(def handler-dev (reload/wrap-reload handler ["src-clj"]))

(defn -main [& opts]
  (print (achievement/all-unused-achievements))
  (db/create-db)
  (db/add-fake-data)
  (jetty/run-jetty handler {:port 8080}))
