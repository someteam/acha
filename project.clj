(defproject acha "0.2.4"
  :description "Enterprise Git Achievements Provider. Web scale. In the cloud"
  :url "http://acha-acha.co"

  :global-vars  {*warn-on-reflection* true}
  :source-paths ["src-clj"]
  :main         acha.server
  :aot          [acha.server]
  :uberjar-name "acha-uber.jar"
  :uberjar-exclusions [#".*\.piko"
                       #"public/out/.*"
                       #"public/index_dev\.html"
                       #"public/react-.*"]

  :dependencies [
    [org.clojure/clojure "1.7.0-alpha5"]
    [org.clojure/tools.logging "0.3.1"]
    [ch.qos.logback/logback-classic "1.1.2"]

    [http-kit "2.1.19"]
    [ring/ring-core "1.3.2" :exclusions [commons-codec org.clojure/tools.reader]]
    [ring/ring-devel "1.3.2"]
    [compojure "1.3.1"]
    [com.cognitect/transit-clj "0.8.259" :exclusions [org.msgpack/msgpack org.clojure/test.check]]

    [clj-jgit "0.8.3" :exclusions [org.clojure/core.memoize]]

    [org.clojure/java.jdbc "0.3.6"]
    [org.xerial/sqlite-jdbc "3.8.7"]
    [com.mchange/c3p0 "0.9.5"]
    
    [org.clojure/clojurescript "0.0-2816"]
    [cljsjs/react-with-addons "0.12.2-4"]
    [rum "0.2.4" :exclusions [cljsjs/react]]

    [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
    [com.cognitect/transit-cljs "0.8.205"]
    [datascript "0.9.0"]
    [sablono "0.3.1" :exclusions [cljsjs/react]]
  ]

  :plugins [
    [lein-ring "0.8.11"]
    [lein-cljsbuild "1.0.4"]
  ]
  :clean-targets ^{:protect false} [
    "target"
    "resources/public/out"
    "resources/public/acha.min.js"
    "resources/public/acha.js"
  ]
  :hooks [leiningen.cljsbuild]
  :cljsbuild { 
    :builds [
      { :id "prod"
        :source-paths ["src-cljs"]
        :jar true
        :compiler {
          :preamble      ["public/md5.js"]
          :output-to     "resources/public/acha.min.js"
          :optimizations :advanced
          :pretty-print  false
        }}
  ]}

 
  :profiles {
    :dev {
      :cljsbuild {
        :builds [
          { :id "dev"
            :source-paths ["src-cljs"]
            :compiler {
              :output-to     "resources/public/acha.js"
              :output-dir    "resources/public/out"
              :optimizations :none
              :source-map    true
            }}
      ]}
    }
  }
)
