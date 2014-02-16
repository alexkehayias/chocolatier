(ns chocolatier.app
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:use-macros [dommy.macros :only [node sel sel1]])
  (:require [clojure.browser.repl :as repl]
            [dommy.core :as dom]
            [chocolatier.engine.core :as engine]))

(def top-nav
  [:div#top-nav [:h1#logo "Chocolatier"]])

(def app-main
  [:div#main])

(defn init-html! []
  (info "Initializing base html")
  (dom/append! (sel1 :body) top-nav)
  (dom/append! (sel1 :body) app-main))

(defn reset-app!
  "Reload the entire application html and canvas app"
  []
  (info "Resetting html")
  (try (do (dom/remove! (sel1 :#top-nav))
           (dom/remove! (sel1 :#main)))
       (catch js/Error e (error e)))
  (init-html!)
  (engine/start-game!))

(reset-app!)
