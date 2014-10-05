(ns chocolatier.engine.components.collidable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]))

(defn exp
  "Raise x to the exponent of n"
  [x n]
  (reduce * (repeat n x)))

(defn halve
  "Divide n by 2"
  [n]
  (/ n 2))

(defn circle-collision?
  "Basic circle collision detection. Returns true if x and y 
   are colliding.

   Two circles are colliding if distance between the center 
   points is less than the sum of the radii."
  [x1 y1 r1 x2 y2 r2]
  (<= (+ (exp (- x2 x1) 2) (exp (- y2 y1) 2))
      (exp (+ r1 r2) 2)))

(defn collision?
  "Compare two entities future position to see if they are colliding. 
   Returns a boolean of whether the two entities are colliding."
  [e1 e2]
  (if (and (seq e1) (seq e2))
    (let [key-list [:screen-x :screen-y
                    :offset-x :offset-y
                    :height :width
                    :hit-radius]
          [x1 y1 off-x1 off-y1 h1 w1 r1] (map #(% e1) key-list)
          [x2 y2 off-x2 off-y2 h2 w2 r2] (map #(% e2) key-list)
          ;; The hit circles are drawn around the center of the entity
          ;; by halving the height and width
          [center-x1 center-y1] (map + [x1 y1] (map halve [w1 h1]))
          [center-x2 center-y2] (map + [x2 y2] (map halve [w2 h2]))          
          ;; Apply offsets of where the two entities will be
          [adj-x1 adj-y1] (map + [center-x1 center-y1] [off-x1 off-y1])
          [adj-x2 adj-y2] (map + [center-x2 center-y2] [off-x2 off-y2])
          colliding? (circle-collision? adj-x1 adj-y1 r1 adj-x2 adj-y2 r2)]
      colliding?)
    false))

(defn include-collidable-entities
  "State parsing function. Returns a vector of component-state
   positions of all collidable entities, component-id and entity-id"
  [state component-id entity-id]
  (let [;; TODO get a list of all entities renderable component state
        entities []
        component-state (ces/get-component-state state component-id entity-id)]
    [entities component-state component-id entity-id]))

(defmulti check-collisions
  (fn [entities component-state component-id entity-id] entity-id))

(defmethod check-collisions :default
  [entities component-state component-id entity-id]
  (ces/mk-component-state component-id entity-id component-state))

(defmethod check-collisions :player1
  [entities component-state component-id entity-id]
  ;; TODO get the player's render state
  ;; TODO exclude the player from collection of collidable entities
  (let [player {}
        collisions (doall (for [e entities] (collision? player e)))
        ;; In order to have a collision the collisions seq must not be
        ;; empty and must have a falsey value
        colliding? (and (every? boolean collisions) (seq collisions))]
    (when colliding? (log/debug collisions "Colliding!!!"))
    (ces/mk-component-state component-id entity-id component-state)))
