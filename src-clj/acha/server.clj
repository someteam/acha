(ns acha.server
  (:require
    [acha.git-parser :as git-parser]
    [acha.achievement :as achievement]
    [acha.dispatcher :as dispatcher]
    [acha.util :as util]
    [acha.db :as db]
    [acha.config :as config]
    [clojure.tools.logging :as logging]
    [clojure.java.io :as io]
    [org.httpkit.server :as server]
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
  (let [url (util/normalize-str url)
        repo (db/get-repo-by-url url)]
    (cond
      (< 4 (db/count-new-repos)) {:repo/status :error, :message "Too many unprocessed repos in queue"}
      repo                      {:repo/status :exists, :repo repo}
      :else                     (let [repo (db/get-or-insert-repo url)]
                                  (logging/info "Added repo:" repo)
                                  {:repo/status :added, :repo repo}))))

(def api-handler
  (->
    (compojure.core/routes
      (compojure.core/GET "/repos/" []
        (db/get-repo-list))
      (compojure.core/GET "/users/" []
        (db/get-user-list))
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

(defn -main [& {:as opts}]
  (let [working-dir (get opts "--dir" ".acha")
        ip          (get opts "--ip" "0.0.0.0")
        port        (-> (get opts "--port" "8080") (Integer/parseInt))
        dev         (contains? opts "--reload")]

    (.mkdir (io/as-file working-dir))
    (alter-var-root #'config/working-dir (fn [_] working-dir))
    (logging/info "Working dir" working-dir)
  
    (db/create-db)
    (dispatcher/run-workers)
    (let [handler (if dev
                    (reload/wrap-reload #'handler {:dirs ["src-clj"]})
                    handler)]
      (server/run-server handler {:port port :ip ip}))
    (logging/info "Server ready at" (str ip ":" port))))
