(defproject chocolatier "0.1.0-SNAPSHOT"
  :description "Chocolatier prototype"
  :source-paths ["src/clj"]

  :dependencies [[org.clojure/clojure "1.7.0"]

                 ;; Web server
                 [ring "1.2.0"]
                 [compojure "1.1.5"]
                 [enlive "1.1.1"]

                 ;; cljs
                 [org.clojure/clojurescript "0.0-3297"]
                 ;; DOM manipulation
                 [prismatic/dommy "1.1.0"
                  :exclude [org.clojure/clojurescript]]

                 ;; TODO deprecate this in favor of the built in testing
                 [com.cemerick/clojurescript.test "0.3.1"
                  :exclude [org.clojure/clojurescript]]

                 [org.clojure/core.async "0.1.346.0-17112a-alpha"
                  :exclude [org.clojure/clojurescript]]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-figwheel "0.3.5"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljs"]
                :figwheel {:on-jsload "chocolatier.core/on-js-reload"}
                :compiler {:main chocolatier.dev
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/chocolatier.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :pretty-print true}}
               {:id "min"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/compiled/chocolatier.js"
                           :main chocolatier.core
                           :optimizations :advanced}}]}

  :figwheel {:http-server-root "public" ;; default and assumes "resources"
             :server-port 3449          ;; default
             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             :nrepl-port 8999

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this
             ;; doesn't work for you just run your own server :)
             ;; :ring-handler hello_world.server/handler

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
