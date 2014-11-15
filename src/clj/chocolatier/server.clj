(ns chocolatier.server
  (:use [chocolatier.dev :only [reset-brepl-env!
                                connect-to-brepl
                                get-repl-client-js]])
  (:require [cemerick.austin.repls :refer (browser-connected-repl-js)]
            [net.cgrand.enlive-html :as enlive]
            [compojure.route :refer (resources)]
            [compojure.core :refer (GET defroutes)]
            ring.adapter.jetty))

(def server (atom nil))

(def project-root
  (str (System/getProperty "user.dir") "/resources/public"))

(def source-root
  (str (System/getProperty "user.dir") "/*"))

(enlive/deftemplate homepage "homepage.html" []
  [:head] (enlive/append
           (enlive/html [:link {:rel "stylesheet"
                                :type "text/css"
                                :href "/static/styles/master.css"}])))

(enlive/deftemplate app "app.html"
  []
  [:body] (enlive/append
           (enlive/html [:script (get-repl-client-js)])
           (enlive/html [:script (browser-connected-repl-js)])))

(defn source-files []
  #(slurp (:uri %)))

(defroutes site
  (resources "/static")
  (GET "/app" req (app))
  ;; TODO disable based on env
  ;; HACK for serving source maps via the server
  (GET source-root req (source-files))
  ;;(GET "/*" req (homepage))
  )

(defn start-server!
  "Returns a server obj"
  [& [options]]
  (let [options (merge {:port 9000 :join? false} options) 
        server (ring.adapter.jetty/run-jetty #'site options)]
    (connect-to-brepl (reset-brepl-env!))
    #(.stop server)))

(defn stop-server! []
  (when @server (@server)))

(defn restart-server!
  []
  (when @server (stop-server!))
  (reset! server (start-server!)))
