(defproject chocolatier "0.1.0-SNAPSHOT"
  :description "Chocolatier prototype"
  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj" "test/cljs"]
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; Web server
                 [ring "1.2.0"]
                 [compojure "1.1.5"]
                 [enlive "1.1.1"]

                 ;; cljs
                 [org.clojure/clojurescript "1.9.293"]
                 ;; DOM manipulation
                 [prismatic/dommy "1.1.0"
                  :exclude [org.clojure/clojurescript]]

                 [org.clojure/core.async "0.1.346.0-17112a-alpha"
                  :exclude [org.clojure/clojurescript]]

                 ;; Devcards
                 [devcards "0.2.2"]

                 ;; State inspection
                 [praline "0.1.0-SNAPSHOT"]

                 ;; Fix for java11 figwheel compatibility issue
                 [javax.xml.bind/jaxb-api "2.4.0-b180830.0359"]]

  :plugins [[lein-cljsbuild "1.1.1" :exclude [org.clojure/clojurescript]]
            [lein-figwheel "0.5.8" :exclude [org.clojure/clojurescript]]
            [refactor-nrepl "1.1.0"]]

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"
                                   :exclude [org.clojure/clojurescript]]
                                  [org.clojure/tools.nrepl "0.2.12"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljs"
                               "test/cljs"]
                :figwheel {:devcards true
                           :autoload true
                           :load-warninged-code true
                           :heads-up-display true
                           :on-jsload "chocolatier.examples.action-rpg.core/on-js-reload"}
                :compiler {:main "chocolatier.devcards"
                           :asset-path "/js/compiled/out"
                           :output-to "resources/public/js/compiled/chocolatier.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :pretty-print true}}
               {:id "min"
                :source-paths ["src/cljs"]
                :compiler {:main "chocolatier.examples.action-rpg.core"
                           :output-to "resources/public/js/compiled/chocolatier-min.js"
                           :externs ["resources/public/js/externs/pixi.js"
                                     "resources/public/js/externs/howler.js"
                                     "resources/public/js/externs/stats.js"
                                     "resources/public/js/externs/rbush.js"]
                           ;; Remove runtime assertions
                           :elide-asserts true
                           :optimizations :advanced
                           :parallel-build true
                           :verbose true
                           ;; Optimize nested function calls
                           :static-fns true}}]}

  :figwheel {:http-server-root "public" ;; default and assumes "resources"
             :server-port 1223          ;; default
             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             :nrepl-port 8999
             ;; Need this now to get nrepl to work
             :nrepl-middleware ["cider.nrepl/cider-middleware"
                                "refactor-nrepl.middleware/wrap-refactor"
                                "cemerick.piggieback/wrap-cljs-repl"]

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this
             ;; doesn't work for you just run your own server :)
             :ring-handler chocolatier.server/app

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"
             }
  )
