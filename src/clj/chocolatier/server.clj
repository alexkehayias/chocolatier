(ns chocolatier.server
  (:use [chocolatier.dev :only [reset-brepl-env! connect-to-brepl]])
  (:require [cemerick.austin.repls :refer (browser-connected-repl-js)]
            [net.cgrand.enlive-html :as enlive]
            [compojure.route :refer (resources)]
            [compojure.core :refer (GET defroutes)]
            ring.adapter.jetty)) 

(def project-root
  (str (System/getProperty "user.dir") "/resources/public"))

(def source-root
  (str (System/getProperty "user.dir") "/*"))

(enlive/deftemplate homepage "homepage.html" []
  [:head] (enlive/append
           (enlive/html [:link {:rel "stylesheet"
                                :type "text/css"
                                :href "/static/styles/master.css"}])) )

(enlive/deftemplate app "app.html"
  []
  [:head] (enlive/append
           (enlive/html [:link {:rel "stylesheet"
                                :type "text/css"
                                :href "/static/styles/master.css"}]))
  [:body] (enlive/append
           (enlive/html [:script (browser-connected-repl-js)])))

(defn source-files []
  #(slurp (:uri %)))

(defroutes site
  (resources "/static")
  (GET "/app" req (app))
  ;; TODO disable based on env
  (GET source-root req (source-files))
  ;;(GET "/*" req (homepage))
  )

(defonce server
  (let [server (ring.adapter.jetty/run-jetty #'site {:port 9000 :join? false})]
    (connect-to-brepl (reset-brepl-env!))
    #(.stop server)))
