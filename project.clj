(defproject chocolatier "0.1.0-SNAPSHOT"
  :description "Chocolatier prototype"
  :source-paths ["src/clj" "src/cljs"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1889"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [ring "1.2.0"]
                 [compojure "1.1.5"]
                 [enlive "1.1.1"]
                 [prismatic/dommy "0.1.1"]]
  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.1"]
                             [lein-cljsbuild "0.3.2"]]
                   :cljsbuild {:builds [{:source-paths ["src/cljs"]
                                         :compiler {:output-to "resources/public/scripts/app.js"
                                                    :optimizations :simple
                                                    :pretty-print true
                                                    :source-map "resources/public/scripts/app.js.map"}}]}}})
