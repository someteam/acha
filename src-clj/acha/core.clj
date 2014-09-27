(ns acha.core
  (:require
    [clojure.tools.logging :as logging]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.reload :as reload]
    [hiccup core page form]
    [compojure core handler route])
  (:gen-class))

(defn- main-page []
  (logging/info "Main page rendering")
  (hiccup.page/html5
    [:head
     [:title "Acha-Acha"]]
    [:body
     [:div.container
      [:h1 "Acha-Acha. Enterprise Git Achievements Provider. Web scale. In the cloud."]]]))

(def handler (compojure.core/routes
               (compojure.route/resources "/")
               (compojure.core/GET "/" []
                 (main-page))
               (compojure.route/not-found "Page not found")))
(def handler-dev (reload/wrap-reload handler ["src-clj"]))

(defn -main [& opts]
  (jetty/run-jetty handler {:port 8080}))
