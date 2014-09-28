(ns acha.server
  (:require
    [acha.git-parser :as git-parser]
    [acha.achievement :as achievement]
    [acha.dispatcher :as dispatcher]
    [acha.util :as util]
    [acha.db :as db]
    [clojure.tools.logging :as logging]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.reload :as reload]
    [ring.middleware.params :as params]
    [cognitect.transit :as transit]
    [ring.util.response :as response]
    [compojure core route])
  (:import
    [java.io ByteArrayOutputStream ByteArrayInputStream])
  (:gen-class))

(defn write-transit [x t opts]
  (let [baos (ByteArrayOutputStream.)
        w    (transit/writer baos t opts)]
    (transit/write w x)
    (ByteArrayInputStream. (.toByteArray baos))))

(defn wrap-transit-response [handler]
  (fn [request]
    (-> (handler request)
       (update-in [:headers] assoc "Content-Type" "application/transit+json; charset=utf-8")
       (update-in [:body] write-transit :json {}))))

(defn add-repo [url]
  (let [url (util/normalize-str url)]
    (if-let [repo (db/get-repo-by-url url)]
      {:repo/status :exists, :repo   repo}
      (let [repo (db/get-or-insert-repo url)]
        (logging/info "Added repo:" repo)
        {:repo/status :added, :repo   repo}))))

(def api-handler
  (->
    (compojure.core/routes
      (compojure.core/GET "/repos/" []
        (db/get-repo-list))
      (compojure.core/GET ["/repos/:id", :id #"[0-9]+"] [id]
        (db/get-repo-ach id))
      (compojure.core/GET "/users/" []
        (db/get-user-list))
      (compojure.core/GET ["/users/:id", :id #"[0-9]+"] [id]
        (db/get-user-ach id))
      (compojure.core/GET "/ach/" []
        (db/get-ach-list))
      (compojure.core/GET "/ach-dir/" []
        {:body acha.achievement-static/table})
      (compojure.core/POST "/add-repo/" [:as req]
        {:body (add-repo (get-in req [:params "url"]))}))
   wrap-transit-response
   params/wrap-params))

(def handler
  (->
    (compojure.core/routes
      (compojure.route/resources "/")
      (compojure.core/GET "/" []
        (response/content-type (response/resource-response "public/index.html") "text/html"))
      (compojure.core/context "/api" []
        api-handler)
      (compojure.route/not-found "Page not found"))
    ))

(def handler-dev (reload/wrap-reload handler ["src-clj"]))

(defn -main [& opts]
;  (print "Not implemented/over implemented achievement lists:")
;  (print achievement/all-unused-achievements)
  (db/create-db)
;  (db/add-fake-data)
  (dispatcher/run-workers)
  (jetty/run-jetty handler {:port 8080}))
