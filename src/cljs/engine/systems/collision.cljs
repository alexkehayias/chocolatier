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
  "Basic circle collision detection. Returns true if x and y are colliding.

   Two circles are colliding if the delta of x squared + the
   difference of y squared is less than or equal to radius squared
   between two circles"
  [x1 y1 r1 x2 y2 r2]
  (<= (+ (exp (- x2 x1) 2) (exp (- y1 y2) 2))
      (exp (+ r1 r2) 2)))

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
  (swap! (:entities state) #(map (check-collisions state time) %)))
