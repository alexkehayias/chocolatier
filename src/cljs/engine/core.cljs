(ns chocolatier.engine.core
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [cljs.core.async :refer [chan <! >!]]
            [dommy.core :as dom])
  (:use-macros [dommy.macros :only [node sel sel1]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; Global game state
(def systems (atom []))
(def entities (atom []))
(def paused (atom false))

;; TODO make a record and protocol for an entity
(defn create-entity!
  [stage img]
  (let [texture (js/PIXI.Texture.fromImage img)
        sprite (js/PIXI.Sprite. texture)]
    (set! (.-position.x sprite) 500)
    (set! (.-position.y sprite) 300)
    (set! (.-anchor.x sprite) .05)
    (set! (.-anchor.y sprite) .05)
    (swap! entities conj sprite)))

(defn render-entity [stage e]
  (.addChild stage e))

(defn draw
  "Renders all the things to the screen.

   Iterates through all the entities and renders them to the stage.
   Renders the stage to the screen"
  [renderer stage]
  (info "draw function called")
  (when-not @paused
    (doseq [e @entities]
          (render-entity stage e))
    (.render renderer stage)))

(defn update-entity [entity]
  ;; (-> entity
  ;;     update-movement
  ;;     update-collisions
  ;;     )
  (aset entity "rotation" (+ 0.05 (aget entity "rotation")))
  entity)

(defn tick-game
  "Tick the game by miliseconds of time"
  [time]
  ;; Update the global entities state atom 
  (swap! entities #(map update-entity %)))

(defn game-loop
  "Calculate changes based on the time since last change"
  []
  ;; TODO Calculate the changes since the last game tick
  ;; TODO should this be async? Allows skipping frames
  (.setInterval js/window #(tick-game time) (/ 1000 60)))

(def stage (js/PIXI.Stage. 0x66ff99))

(defn render-loop
  "Renders the game every n seconds"
  []
  (let [;; TODO get the screen height and width
        ;; TODO allow the screen height and width to be variable?
        width 800
        height 600
        renderer (js/PIXI.CanvasRenderer. width height)]
    (dom/append! (sel1 :body) (.-view renderer))
    (.setInterval js/window #(draw renderer stage) (/ 1000 60))))

(defn start []
  (create-entity! stage "static/images/bunny.png")
  (render-loop)
  (game-loop))
