(ns acha.core
  (:require
    [clojure.tools.logging :as logging]
    [compojure core handler route]))

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
