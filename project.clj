(defproject acha "0.2.2"
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
    [org.clojure/clojure "1.7.0-alpha2"]
    [org.clojure/tools.logging "0.3.1"]
    [ch.qos.logback/logback-classic "1.1.2"]

    [http-kit "2.1.19"]
    [ring/ring-core "1.3.1" :exclusions [commons-codec org.clojure/tools.reader]]
    [ring/ring-devel "1.3.1"]
    [compojure "1.2.0"]
    [com.cognitect/transit-clj "0.8.259" :exclusions [org.msgpack/msgpack org.clojure/test.check]]

    [clj-jgit "0.7.6" :exclusions [org.clojure/core.memoize]]

    [org.clojure/java.jdbc "0.3.5"]
    [org.xerial/sqlite-jdbc "3.7.2"]
    [com.mchange/c3p0 "0.9.2.1"]
    
    [org.clojure/clojurescript "0.0-2356" :exclusions [org.mozilla/rhino]]
    [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
    [com.cognitect/transit-cljs "0.8.188"]
    [datascript "0.4.1" :exclusions [org.clojure/clojurescript]]
    [sablono "0.2.22"]
    [com.facebook/react "0.11.2"]
  ]

  :plugins [
    [lein-ring "0.8.11"]
    [lein-cljsbuild "1.0.3"]
  ]

  :cljsbuild { 
    :builds [
      { :id "prod"
        :source-paths ["src-cljs"]
        :compiler {
          :externs       ["react/externs/react.js" "datascript/externs.js"]
          :preamble      ["react/react_with_addons.min.js" "public/md5.js"]
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
