(ns chocolatier.engine.systems.debug
  (:use [chocolatier.utils.logging :only [debug error]])
  (:require [chocolatier.engine.components :refer [Collidable]]
            [chocolatier.engine.state :as s]))


;; Graphics object is mutated and stored here
(def hit-zones (atom nil))

(defn init-graphic!
  "Initialize a new PIXI Graphics object into the stage"
  [stage]
  (println "Initializing hitzone graphics")
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
  (.drawCircle graphic
               (:screen-x entity)
               (:screen-y entity)
               ;; TODO set the radius based on the size of the entity
               30))

(defn show-hit-zone-system
  "Display a circle around all hit zones for each entity"
  [state time]
  ;; Statefully clear existing visualization of hit zones
  (if (nil? @hit-zones)
    (reset! hit-zones (init-graphic! (:stage @(:game state))))
    ;; Clear out the old graphic with a new one
    (.clear @hit-zones))

  ;; Draw circles to visualize the collision area
  (doseq [entity @(:entities state)]
    (when (satisfies? Collidable entity)
      ;; TODO this doesn't work for the player since their screen x
      ;; and screen y do not mean they are offset on the screen
      (draw-hit-zone @hit-zones entity))))
