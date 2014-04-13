(ns chocolatier.engine.systems.debug
  (:use [chocolatier.utils.logging :only [debug error]])
  (:require [chocolatier.engine.components :refer [Collidable]]
            [chocolatier.engine.state :as s]))


(defn init-graphic!
  "Initialize a new PIXI Graphics object into the stage"
  [stage]
  (debug "Initializing hitzone graphics")
  (let [graphic (new js/PIXI.Graphics)]
    (.addChild stage graphic)
    graphic))

(defn base-style
  "Applies styles to the graphic."
  [graphic]
  (.lineStyle graphic 0)
  (.beginFill graphic 0xFFFF0B 0.5)
  graphic)

(defn draw-hit-zone
  "Draws the entities hitzone circle to a graphics object"
  [graphic entity]
  (base-style graphic)
  (let [{:keys [screen-x screen-y hit-radius sprite]} entity
        ;; Offset so it's in the hitzone is centered on the entity
        half-height (/ (aget sprite "height") 2) 
        half-width (/ (aget sprite "width") 2)
        x (+ screen-x half-width)
        y (+ screen-y half-height)]
    (.drawCircle graphic x y hit-radius)))

(defn show-hit-zone-system
  "Display a circle around all hit zones for each entity"
  [state time]

  ;; If the s/hit-zones graphic does not exist, create it
  (if (nil? @s/hit-zones)
    (reset! s/hit-zones (init-graphic! (:stage @(:game state))))
    ;; Clear out the old graphic with a new one
    (.clear @s/hit-zones))

  ;; Draw circles to visualize the collision area
  (doseq [[id entity] (seq @(:entities state))]
    (when (satisfies? Collidable entity)
      ;; TODO this doesn't work for the player since their screen x
      ;; and screen y do not mean they are offset on the screen
      (draw-hit-zone @s/hit-zones entity))))
