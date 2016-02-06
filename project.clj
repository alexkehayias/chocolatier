(defproject chocolatier "0.1.0-SNAPSHOT"
  :description "Chocolatier prototype"
  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj" "test/cljs"]
  :dependencies [[org.clojure/clojure "1.7.0"]

                 ;; Web server
                 [ring "1.2.0"]
                 [compojure "1.1.5"]
                 [enlive "1.1.1"]

                 ;; cljs
                 [org.clojure/clojurescript "1.7.228"]
                 ;; DOM manipulation
                 [prismatic/dommy "1.1.0"
                  :exclude [org.clojure/clojurescript]]

                 [org.clojure/core.async "0.1.346.0-17112a-alpha"
                  :exclude [org.clojure/clojurescript]]

                 ;; Devcards
                 [devcards "0.2.1-4"]]

  :plugins [[lein-cljsbuild "1.1.1" :exclude [org.clojure/clojurescript]]
            [lein-figwheel "0.5.0-1" :exclude [org.clojure/clojurescript]]
            [refactor-nrepl "1.1.0"]]

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"
                                   :exclude [org.clojure/clojurescript]]
                                  [org.clojure/tools.nrepl "0.2.12"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   }}

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljs"]
                :figwheel {:on-jsload "chocolatier.core/on-js-reload"
                           :devcards true}
                :compiler {:main chocolatier.dev
                           :asset-path "/js/compiled/out"
                           :output-to "resources/public/js/compiled/chocolatier.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :pretty-print true}}
               {:id "min"
                :source-paths ["src/cljs"]
                :compiler {:main chocolatier.dev
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
                           :static-fns true}}
               {:id "devcards"
                :source-paths ["src/cljs"]
                :figwheel {:devcards true}
                :compiler {:main chocolatier.devcards
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/chocolatier-devcards.js"
                           :source-map-timestamp true
                           :parallel-build true
                           :pretty-print true}}]}

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
