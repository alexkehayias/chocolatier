(ns chocolatier.engine.systems.collision
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.state :as s]
            [chocolatier.engine.components :refer [Collidable]]
            [chocolatier.engine.components :as c]))


(defn collision-system
  "Calls all UserCollision entities to react to user collision"
  [state time]
  (doseq [entity @(:entities state)]
    (when (satisfies? Collidable entity)
      (c/check-collision entity state time))))


(defn draw-circle
  "Draw a circle to the screen at screen position x y with radius r
   Returns a PIXI graphic object"
  [stage x y r]
  (let [g (new js/PIXI.Graphics)]
    (.lineStyle g 0)
    (.beginFill g 0xFFFF0B, 0.5)
    (.drawCircle g x y r)
    (debug "Adding circle to stage")
    (.addChild stage g)
    g))

(defn debug-collision-system
  "Draw hit boxes around the entities"
  [state time]
  (doseq [entity @(:entities state)
          stage (:stage @(:game state))]
    ;; Draw a box
    (when (satisfies? Collidable entity)
      (draw-circle stage (:screen-x entity) (:screen-y entity) 100))))
