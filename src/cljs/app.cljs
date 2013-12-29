(ns chocolatier.app
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:use-macros [dommy.macros :only [node sel sel1]])
  (:require [clojure.browser.repl :as repl]
            [dommy.core :as dom]))

(def top-nav
  [:div#top-nav [:h1#logo "Chocolatier"]])

(def app-main
  [:div#main "hello this is the main"])

(defn init-html! []
  (info "Initializing base html")
  (dom/append! (sel1 :body) top-nav)
  (dom/append! (sel1 :body) app-main))

(defn set-position [sprite x y]
  (set! (.-position.x sprite) x)
  (set! (.-position.y sprite) y))

(defn set-anchor [sprite x y]
  (set! (.-anchor.x sprite) x)
  (set! (.-anchor.y sprite) y))

(defn empty-stage [stage]
  (doall (map
          (fn [target]
            (.log js/console target)
            (. stage removeChild target))
             ;; do a slice 0 here to copy the array as the stage array
          ;; mutates with removes
          (.slice (.-children stage) 0))))

(defn update-world [bunny]
  (aset bunny "rotation" (+ 0.05 (aget bunny "rotation"))))

(defn animate [renderer stage bunny]
  #(do
     (js/requestAnimFrame (animate renderer stage bunny))
     (update-world bunny)
     (. renderer render stage)))

(defn create-simple [stage sprite]
  (set-position sprite 200 100)
  (set-anchor sprite 0.5 0.5)
  (. stage addChild sprite)
  sprite)

(defn init-app! []
  ;; FIX use webgl with canvas fallback
  ;; Can't restart the app due to texture cache being specific to a
  ;; webgl context, so resetting the app via repl causes an error
  (let [renderer (js/PIXI.CanvasRenderer. 400 300)
        stage (js/PIXI.Stage. 0x66ff99)
        bunny-texture (js/PIXI.Texture.fromImage "static/images/bunny.png")
        bunny (js/PIXI.Sprite. bunny-texture)]
    (dom/append! (sel1 :#main) (.-view renderer))
    (empty-stage stage)
    (create-simple stage bunny)
    ;; TODO replace this with a draw loop
    (js/requestAnimFrame (animate renderer stage bunny))))

(defn reset-app!
  "Reload dom first then bind events"
  []
  (info "Resetting html")
  (try (do (dom/remove! (sel1 :#top-nav))
           (dom/remove! (sel1 :#main)))
       (catch js/Error e (error e)))
  (init-html!)
  (info "Resetting canvas")
  (init-app!))

(reset-app!)
