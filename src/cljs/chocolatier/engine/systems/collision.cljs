(ns chocolatier.engine.systems.collision
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.state :as s]
            [chocolatier.engine.components :refer [Collidable]]
            [chocolatier.engine.components :as c]))


(defn exp
  "Raise x to the exponent of n"
  [x n]
  (reduce * (repeat n x)))

(defn collision?
  "Basic circle collision detection. Returns true if x and y 
   are colliding.

   Two circles are colliding if distance between the center 
   points is less than the sum of the radii."
  [x1 y1 r1 x2 y2 r2]
  ;; (debug "Comparing circle" x1 y1 r1 "to" x2 y2 r2)
  (<= (+ (exp (- x2 x1) 2) (exp (- y2 y1) 2))
      (exp (+ r1 r2) 2)))

(defn entity-collision?
  "Compare two entities to see if they are colliding. Returns a boolean."
  [e1 e2]
  (if (and (satisfies? Collidable e1) (satisfies? Collidable e2)) 
    (let [{:keys [screen-x screen-y offset-x offset-y hit-radius]} e1
          ;; Apply the offsets as if they were happening
          [x1 y1] (map + [screen-x screen-y] [offset-x offset-y])
          r1 hit-radius 
          {:keys [screen-x screen-y offset-x offset-y hit-radius]} e2
          [x2 y2] (map + [screen-x screen-y] [offset-x offset-y])
          r2 hit-radius
          colliding? (collision? x1 y1 r1 x2 y2 r2)]
      (when colliding?
        (debug "Collision detected"
               (:id e1) (:id e2)))
      colliding?)
    false))

(defn check-collisions
  "Returns a function that takes an entity as an argument for checking 
   collisions. Initialized with game state and time interval."
  [state time]
  (fn [entity]
    (if (satisfies? Collidable entity)
      (c/check-collision entity state time)
      entity)))

(defn collision-system
  [state time]
  (swap! (:entities state)
         ;; Need to force evaluation since this is lazy
         #(doall (map (check-collisions state time) %))))
