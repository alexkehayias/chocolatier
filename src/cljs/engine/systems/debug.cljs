(ns chocolatier.engine.systems.debug
  (:use [chocolatier.utils.logging :only [debug error]])
  (:require [chocolatier.engine.components :refer [Collidable]]
            [chocolatier.engine.state :as s]))


;; Graphics object is mutated and stored here
(def hit-zones (atom nil))

(defn init-graphic! [stage]
  (let [g (new js/PIXI.Graphics)]
    (.lineStyle g 0)
    (.beginFill g 0xFFFF0B 0.5)    
    (.addChild stage g)
    g))

(defn show-hit-zone-system
  "Display a circle around all hit zones for each entity"
  [state time]
  ;; Statefully clear existing visualization of hit zones
  (when-not (nil? @hit-zones)
    (.clear @hit-zones))
  ;; Clear out the old graphic with a new one
  (reset! hit-zones (init-graphic! (:stage @(:game state))))
  ;; Draw circles to visualize the collision area
  (doseq [entity @(:entities state)]
    (when (satisfies? Collidable entity)
      ;; TODO this doesn't work for the player since their screen x
      ;; and screen y do not mean they are offset on the screen
      (.drawCircle @hit-zones
                   (:screen-x entity)
                   (:screen-y entity)
                   30))))
