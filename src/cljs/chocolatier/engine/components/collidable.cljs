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

(defn include-collidable-entities
  "State parsing function. Returns a vector of component-state
   positions of all collidable entities and their id, component-id and this entity-id"
  [state component-id entity-id]
  (let [entity-ids (ces/entities-with-component (:entities state) component-id)
        ;; Only want the renderable component state as that has the
        ;; actual sprites with real positions
        ;; Add on the id of the entity
        entities (map #(assoc (ces/get-component-state state :renderable %) :id %)
                      entity-ids)
        component-state (ces/get-component-state state component-id entity-id)
        inbox (ces/get-event-inbox state component-id entity-id)]
    [entities component-state component-id entity-id inbox]))

(defmulti check-collisions
  "Returns updated component state and collision events when colliding"
  (fn [entities component-state component-id entity-id inbox] entity-id))

(defmethod check-collisions :default
  [entities component-state component-id entity-id inbox]
  component-state)


;; Get messages from controllable, apply the offsets to the
;; position of the target entity-id this will indicate that the entity
;; WILL collide if it moves to the intended position
(defmethod check-collisions :player1
  [entities component-state component-id entity-id inbox]
  (let [input-change (filter #(= (:event-id %) :input-change) inbox)]
    ;; If the player has not moved don't bother with collision
    ;; detection that way each entity is in charge of their own
    ;; collision detection
    (if-not (seq input-change)
      component-state
      (let [offsets (apply merge-with + (map :msg inbox))
            player (first (filter #(= (:id %) entity-id) entities))
            player-with-offsets (merge player offsets)
            ;; Exclude the player from collection of collidable entities
            filtered-entities (filter #(not= (:id %) entity-id) entities)
            collisions (doall (for [e filtered-entities]
                                (collision? player-with-offsets e)))
            ;; In order to have a collision the collisions seq must not be
            ;; empty and must have a true value
            colliding? (some #{true} collisions)]
        (if colliding?
          [component-state [[:collision entity-id {:colliding? true}]]]
          component-state)))))
