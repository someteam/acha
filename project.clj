(defproject acha "0.1.0-SNAPSHOT"
  :global-vars    {*warn-on-reflection* true}
  :source-paths   ["src-clj"]
  :description "Enterprise Git Achievements Provider. Web scale. In the cloud"
  :url "http://acha.clojurecup.com"
  :dependencies [
    [org.clojure/clojure "1.6.0"]
    [org.clojure/tools.logging "0.3.0"]

    [ring "1.3.0"]
    [compojure "1.1.8"]
    [hiccup "1.0.5"]

    [clj-jgit "0.7.6"]
  ]

  :plugins [
    [lein-ring "0.8.11"]
    [lein-cljsbuild "1.0.3"]]

  :main acha.core
  :profiles {
    :dev {
      :ring { :handler acha.core/handler-dev }
    }
  })
