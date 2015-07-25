(ns chocolatier.engine.systems.collision
  "System for checking collisions between entities"
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))


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
  "Returns a collection of events."
  [entities]
  ;; Loop through so we don't check entities more than once
  (loop [entities entities
         accum []]
    (if (empty? entities)
      accum
      (recur
       (rest entities)
       (reduce into accum
               (for [other-entity entities
                     :let [entity (first entities)]
                     :when (and (not= entity other-entity)
                                (collision? entity other-entity))]
                 ;; Emit a message for both entities that collided
                 [(ev/mk-event {:colliding? true} :collision (:id entity))
                  (ev/mk-event {:colliding? true} :collision (:id other-entity))]))))))

(defn narrow-collision-system
  "Performs narrow collision detection between entities in each cell of the spatial
   grid where there are more than one entities."
  [state]
  (let [events (for [[coords entities] (-> state :state :spatial-grid)
                     :let [collisions (check-collisions entities)]
                     :when (seq collisions)]
                 collisions)]
    (ev/emit-events state (reduce into [] events))))

(defn mk-spatial-grid
  "Returns a hashmap representing a spatial grid"
  [entities cell-size]
  (group-by (fn [entity]
              (let [{:keys [pos-x pos-y]} entity
                    col (js/Math.floor (/ pos-x cell-size))
                    row (js/Math.floor (/ pos-y cell-size))]
                (str col ":" row)))
            entities))

(defn get-multi-component-state
  "Returns a collection of hashmaps of component state. Append an :id field
   for the entity's unique ID"
  [state component-ids entity-ids]
  (map
   (fn [id]
     (into {:id id} (map #(ces/get-component-state state % id) component-ids)))
   entity-ids))


(defn broad-collision-system
  "Returns a function that divides entities into a spatial grid based on their screen
   position. Takes the screen height width and dimension of cells."
  [cell-size]
  (fn [state]
    (let [;; Get only the entities that are collidable and moveable
          component-ids [:collidable :moveable]
          entity-ids (ces/entities-with-multi-components (:entities state)
                                                         component-ids)
          entity-state (get-multi-component-state state component-ids entity-ids)
          grid (mk-spatial-grid entity-state cell-size)]
      (assoc-in state [:state :spatial-grid] grid))))
