(ns chocolatier.engine.systems.collision
  "System for checking collisions between entities"
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ecs :as ecs]
            [chocolatier.engine.events :as ev]))


(defn get-component-state
  "Returns a collection of vectors of id, move-state, collision-state for each
   entity-ids"
  [state entity-ids]
  (let [collidable-state (ecs/get-all-component-state state :collidable)
        moveable-state (ecs/get-all-component-state state :moveable)]
    (for [e entity-ids]
      [e (get moveable-state e) (get collidable-state e)])))

(defn rbush
  ([]
   (js/rbush))
  ([max-entries]
   (js/rbush max-entries)))

(defn rbush-item
  ([x1 y1 x2 y2]
   (rbush-item x1 y1 x2 y2 nil))
  ([x1 y1 x2 y2 data]
   (array x1 y1 x2 y2 data)))

(defn rbush-insert
  [rbush item]
  (.insert rbush item))

(defn rbush-remove
  [rbush item]
  (.remove rbush item))

(defn rbush-clear
  [rbush]
  (.clear rbush))

(defn rbush-bulk-insert
  [rbush items]
  (.load rbush items))

(defn rbush-collides?
  [rbush item]
  (.collides rbush item))

(defn rbush-search
  [rbush item]
  (.search rbush item))

(defn rbush-all
  [rbush]
  (.all rbush))

(defn entity-state->bounding-box
  "Format entity state for use with spatial index. Includes all
   collision component state in the metadata of the spatial tree item"
  [id {:keys [pos-x pos-y offset-x offset-y]} {:keys [height width attributes]}]
  (rbush-item pos-x
              pos-y
              ;; Adjust the width and height of the bounding box based
              ;; on where the entity will be next frame
              (+ pos-x width offset-x)
              (+ pos-y height offset-y)
              ;; Include the parent ID so we can pass information
              ;; about who created this collidable entity
              {:id id :attributes attributes}))

(defn update-spatial-index
  [index entity-states]
  (let [items (array)]
    ;; YUK! This is to prevent having to call clj->js which is slow so
    ;; we will use side effects instead
    (doseq [[id move-state collision-state] entity-states]
      (.push items (entity-state->bounding-box id move-state collision-state)))
    (-> index
        (rbush-clear)
        (rbush-bulk-insert items))))

(def spatial-index-location
  [:state :spatial-index])

(defn mk-broad-collision-system
  [max-entries]
  (fn [state]
    (let [;; Get only the entities that are both collidable and moveable
          entity-ids (ecs/entities-with-multi-components state [:collidable :moveable])
          entity-states (get-component-state state entity-ids)
          ;; Get or create the spatial index
          spatial-index (get-in state spatial-index-location (rbush max-entries))]
      (assoc-in state spatial-index-location
                (update-spatial-index spatial-index entity-states)))))

(def collision-queue-path
  (conj ev/queue-path :collision))

(defn attack?
  "Returns a boolean of whether the id is an attack"
  [attributes]
  (contains? attributes :damage))

(defn valid-collision-item?
  "Returns a function parameterized by the viewport height and width
   that returns true if the spatial index item, a four element js
   array, is valid for checking collisions against.

   Excludes:
   - Items that are not in the view port
   - Items whose ID starts with attack (attacks shouldn't collide with attacks)
   - Items with a from ID that is the same as the entity-id (immune to your
     own attacks)"
  [[x y _ _ {:keys [id attributes]}] width height]
  (and (< x width)
       (< y height)
       (not (attack? attributes))
       (not (keyword-identical? id (:from-id attributes)))))

(defn ^boolean not-self? [id item]
  (not (keyword-identical? id (:id (nth item 4)))))

(defn ^boolean not-self-attack? [id item]
  (not (keyword-identical? id (get-in (nth item 4) [:attributes :from-id]))))

(defn collision-events
  "Returns a hashmap of all collision events by entity ID"
  [collision-items spatial-index width height]
  (loop [items collision-items
         accum (transient {})]
    (let [item (first items)]
      (if item
        (if (valid-collision-item? item width height)
          (let [id (:id (nth item 4))
                collisions (rbush-search spatial-index item)]
            (if (some #(and (not-self? id %) (not-self-attack? id %))
                      collisions)
              (recur (rest items)
                     (assoc! accum id [(ev/mk-event {:collisions collisions}
                                                    [:collision id])]))
              (recur (rest items) accum)))
          (recur (rest items) accum))
        (persistent! accum)))))

(defn mk-narrow-collision-system
  "Returns a function parameterized by the height and width of the game.
   Returns an update game state with collision events emitted for all eligible
   entities stored in the spatial index"
  [height width]
  (fn [state]
    (let [spatial-index (get-in state spatial-index-location)
          items (rbush-all spatial-index)
          events (collision-events items spatial-index width height)]
      (ev/batch-emit-events state [:collision] events))))
