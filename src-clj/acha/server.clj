(ns acha.server
  (:require
    [acha.git-parser :as git-parser]
    [acha.achievement :as achievement]
    [acha.dispatcher :as dispatcher]
    [acha.util :as util]
    [acha.db :as db]
    [acha.core]
    [clojure.tools.logging :as logging]
    [clojure.java.io :as io]
    [clojure.core.async :as async]
    [org.httpkit.server :as server]
    [ring.middleware.reload :as reload]
    [ring.middleware.params :as params]
    [cognitect.transit :as transit]
    [ring.util.response :as response]
    [compojure core route])
  (:import
    [java.io ByteArrayOutputStream ByteArrayInputStream])
  (:gen-class))

(defn write-transit-bytes [x]
  (let [baos (ByteArrayOutputStream.)
        w    (transit/writer baos :json {})]
    (transit/write w x)
    (.toByteArray baos)))

(defn write-transit-str [x]
  (String. ^bytes (write-transit-bytes x) "utf-8"))

(defn wrap-transit-response [handler]
  (fn [request]
    (-> (handler request)
       (update-in [:headers] assoc "Content-Type" "application/transit+json; charset=utf-8")
       (update-in [:body] #(ByteArrayInputStream. (write-transit-bytes %))))))

(defn add-repo [url]
  (if (< 4 (db/count-new-repos))
      (do
        (async/put! acha.core/events
          [{:db/id       2999999
            :repo/status :error
            :repo/reason "Too many unprocessed repos in queue"
            :repo/url    url}])
        {:result :error, :url url, :message "Too many unprocessed repos in queue"})
    (let [repo (db/get-or-insert-repo url)]
      (if (= repo :exists)
        (do
          (async/put! acha.core/events
            [{:db/id       2999999
              :repo/status :error
              :repo/reason "Repository already added"
              :repo/url    url}])
        {:result :exists, :url url})
        (do
          (logging/info "Added repo:" repo)
          {:result :added, :url url})))))

(def api-handler
  (->
    (compojure.core/routes
      (compojure.core/GET "/db/" []
        (concat
          [[:db/add 0 :meta/app-version acha.core/version]
           [:db/add 0 :meta/db-version  (:version (db/db-meta))]]
          (map db/achent->entity acha.achievement-static/table)
          (map db/repo->entity   (db/get-repo-list))
          (map db/user->entity   (db/get-user-list))
          (map db/ach->entity    (db/get-ach-list))))
      (compojure.core/POST "/add-repo/" [:as req]
        {:body (add-repo (get-in req [:params "url"]))}))
   wrap-transit-response
   params/wrap-params))

(defn events-handler [req]
  (server/with-channel req socket
    (let [ch (async/chan)]
      (async/tap acha.core/events-mult ch)
      (async/go-loop []
        (when-let [data (async/<! ch)]
          (server/send! socket (write-transit-str data))
          (recur)))
      (server/on-close socket
        (fn [status]
          (async/untap acha.core/events-mult ch)
          (async/close! ch))))))

(def handler
  (->
    (compojure.core/routes
      (compojure.core/context "/api" []
        api-handler)
      (compojure.core/GET "/events" []
        events-handler)
      (compojure.core/GET "/" []
        (response/content-type (response/resource-response "public/index.html") "text/html"))
      (compojure.route/resources "/")
      (compojure.route/not-found "Page not found"))))

(defn -main [& {:as opts}]
  (let [working-dir (get opts "--dir" acha.core/working-dir)
        ip          (get opts "--ip" "0.0.0.0")
        port        (-> (get opts "--port" "8080") (Integer/parseInt))
        dev         (contains? opts "--reload")]

    (.mkdirs (io/as-file working-dir))
    (alter-var-root #'acha.core/working-dir (fn [_] working-dir))
    (logging/info "Working dir" working-dir)
  
    (db/initialize-db)
    (dispatcher/run-workers)
    (let [handler (if dev
                    (reload/wrap-reload #'handler {:dirs ["src-clj"]})
                    handler)]
      (server/run-server handler {:port port :ip ip}))
    (logging/info "Server ready at" (str ip ":" port))))
