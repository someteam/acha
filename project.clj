(defproject acha "0.1.0-SNAPSHOT"
  :description "Enterprise Git Achievements Provider. Web scale. In the cloud"
  :url "http://acha.clojurecup.com"

  :global-vars    {*warn-on-reflection* true}
  :source-paths   ["src-clj"]
  :main acha.core

  :dependencies [
    [org.clojure/clojure "1.6.0"]
    
    [org.clojure/tools.logging "0.3.1"]

    [ring "1.3.1"]
    [compojure "1.1.9"]

    [clj-jgit "0.7.6"]
    
    [org.clojure/clojurescript "0.0-2356"]
    [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
    [datascript "0.4.1"]
    [sablono "0.2.22"]
    [com.facebook/react "0.11.2"]
  ]

  :plugins [
    [lein-ring "0.8.11"]
    [lein-cljsbuild "1.0.3"]
  ]

  
  
  :cljsbuild { 
    :builds [
      { :id "dev"
        :source-paths ["src"]
        :compiler {
          :output-to     "web/acha.js"
          :output-dir    "web/out"
          :optimizations :none
          :source-map    true
        }}
      { :id "prod"
        :source-paths ["src"]
        :compiler {
          :externs  ["react/externs/react.js" "datascript/externs.js"]
          :preamble ["react/react.min.js"]
          :output-to     "web/achca.min.js"
          :optimizations :advanced
          :pretty-print  false
        }}
  ]}

  
  :profiles {
    :dev {
      :ring { :handler acha.core/handler-dev }
    }
  }
)
