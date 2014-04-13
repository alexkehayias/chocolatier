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

(defn halve
  "Divide n by 2"
  [n]
  (/ n 2))

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
   Returns a boolean of whether the two entities are colliding."
  [e1 e2]
  (if (and (satisfies? Collidable e1) (satisfies? Collidable e2)) 
    (let [key-list [:screen-x :screen-y
                    :offset-x :offset-y
                    :height :width
                    :hit-radius]
          [x1 y1 off-x1 off-y1 r1 h1 w1] (map #(% e1) key-list)
          [x2 y2 off-x2 off-y2 r2 h2 w2] (map #(% e2) key-list)
          ;; The hit circles are drawn around the center of the entity
          ;; by halving the height and width
          [center-x1 center-y1] (map + [x1 y1] (map halve [w1 h1]))
          [center-x2 center-y2] (map + [x2 y2] (map halve [w2 h2]))          
          ;; Apply offsets of where the two entities will be
          [adj-x1 adj-y1] (map + [center-x1 center-y1] [off-x1 off-y1])
          [adj-x2 adj-y2] (map + [center-x2 center-y2] [off-x2 off-y2])
          colliding? (collision? adj-x1 adj-y1 r1 adj-x2 adj-y2 r2)]
      ;; (when colliding? (debug "Collision detected between"
      ;;                         (:id e1) adj-x1 adj-y1 r1 "and"
      ;;                         (:id e2) adj-x2 adj-y2 r2))
      colliding?)
    false))

(defn collision-system
  [state time]
  (swap! (:entities state)
         #(into % (for [[id entity] (seq %)] 
                    (if (satisfies? Collidable entity)
                      [id (c/check-collision entity state time)]
                      [id entity])))))
