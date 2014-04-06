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
  (<= (+ (exp (- x2 x1) 2) (exp (- y2 y1) 2))
      (exp (+ r1 r2) 2)))

(defn entity-collision?
  "Compare two entities future position to see if they are colliding. 
   Returns a boolean."
  [e1 e2]
  (if (and (satisfies? Collidable e1) (satisfies? Collidable e2)) 
    (let [key-list [:screen-x :screen-y :offset-x :offset-y :hit-radius]
          [x1 y1 off-x1 off-y1 r1] (map #(% e1) key-list)
          [x2 y2 off-x2 off-y2 r2] (map #(% e2) key-list)          
          ;; Apply offsets of where the two entities would be
          [adj-x1 adj-y1] (map + [x1 y1] [off-x1 off-y1])
          [adj-x2 adj-y2] (map + [x2 y2] [off-x2 off-y2])
          colliding? (collision? adj-x1 adj-y1 r1 adj-x2 adj-y2 r2)]
      (when colliding? (debug "Collision detected between"
                              (:id e1) adj-x1 adj-y1 r1 "and"
                              (:id e2) adj-x2 adj-y2 r2)
            ;; Pause the game
            (swap! (:game s/state) assoc :paused true)
            )
      ;; Return the results of the collision test
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
