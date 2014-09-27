(ns acha.server
  (:require
    [acha.git-parser :as git-parser]
    [acha.db :as db]
    [clojure.tools.logging :as logging]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.reload :as reload]
    [ring.util.response :as response]
    [compojure core route])
  (:gen-class))

(def handler (compojure.core/routes
               (compojure.route/resources "/")
               (compojure.core/GET "/" []
                 (response/content-type (response/resource-response "public/index.html") "text/html"))
               (compojure.route/not-found "Page not found")))
(def handler-dev (reload/wrap-reload handler ["src-clj"]))

(defn -main [& opts]
  (db/create-db)
  (jetty/run-jetty handler {:port 8080}))
