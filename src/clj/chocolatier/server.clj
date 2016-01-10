(ns chocolatier.server
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.util.response :as response]))

(defroutes app-routes
  (GET "/dev" [] (response/resource-response "dev.html" {:root "public"}))
  (GET "/min" [] (response/resource-response "dev-min.html" {:root "public"}))
  (GET "/dev/cards" [] (response/resource-response "devcards.html" {:root "public"}))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-keyword-params))
