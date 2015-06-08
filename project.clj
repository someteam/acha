(defproject acha "0.2.5"
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
    [org.clojure/clojure "1.7.0-RC1"]
    [org.clojure/tools.logging "0.3.1"]
    [org.clojure/tools.cli "0.3.1"]
    [ch.qos.logback/logback-classic "1.1.3"]

    [http-kit "2.1.19"]
    [ring/ring-core "1.3.2" :exclusions [commons-codec org.clojure/tools.reader]]
    [ring/ring-devel "1.3.2"]
    [compojure "1.3.4"]
    [com.cognitect/transit-clj "0.8.275" :exclusions [org.msgpack/msgpack org.clojure/test.check]]

    [clj-jgit "0.8.8" :exclusions [org.clojure/core.memoize]]

    [org.clojure/java.jdbc "0.3.7"]
    [org.xerial/sqlite-jdbc "3.8.10.1"]
    [com.mchange/c3p0 "0.9.5"]
    
    [org.clojure/clojurescript "0.0-3308"]

    [rum "0.2.6" :exclusions [cljsjs/react]]
    [cljsjs/react-with-addons "0.12.2-7"]

    [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
    [com.cognitect/transit-cljs "0.8.220"]
    [datascript "0.11.4"]
    [sablono "0.3.4" :exclusions [cljsjs/react]]
  ]

  :plugins [
    [lein-ring "0.9.4"]
    [lein-cljsbuild "1.0.6"]
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
