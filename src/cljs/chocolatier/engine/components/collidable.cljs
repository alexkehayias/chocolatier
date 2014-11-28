(ns chocolatier.engine.components.collidable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.events :as ev]))

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
    (let [key-list [:pos-x :pos-y
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
          [adj-x1 adj-y1] (map - [center-x1 center-y1] [off-x1 off-y1])
          [adj-x2 adj-y2] (map - [center-x2 center-y2] [off-x2 off-y2])
          colliding? (circle-collision? adj-x1 adj-y1 r1 adj-x2 adj-y2 r2)]
      colliding?)
    false))

(defn check-collisions
  "Get move-change events and apply the offsets to the
   position of the target entity-id this will indicate that the entity
   WILL collide if it moves to the intended position"
  [entity-id component-state inbox & {:as sys-kwargs}]
  (let [{entities :entities-x} sys-kwargs
        input-change (filter #(= (:event-id %) :move-change) inbox)]
    ;; If this-entity has not moved don't bother with collision
    ;; detection that way each entity is in charge of their own
    ;; collision detection
    (if-not (seq input-change)
      component-state
      (let [offsets (apply merge-with + (map :msg inbox))
            this-entity (first (filter #(= (:id %) entity-id) entities))
            this-entity-with-offsets (merge this-entity offsets)
            ;; Exclude this-entity from collection of collidable entities
            filtered-entities (filter #(not= (:id %) entity-id) entities)
            collisions (doall (for [e filtered-entities]
                                (collision? this-entity-with-offsets e)))
            ;; In order to have a collision the collisions seq must not be
            ;; empty and must have a true value
            colliding? (some #{true} collisions)]
        (if colliding?
          [component-state [(ev/mk-event {:colliding? true} :collision entity-id)]]
          component-state)))))
