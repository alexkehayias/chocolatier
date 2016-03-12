(ns chocolatier.engine.systems.collision
  "System for checking collisions between entities"
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ecs :as ecs]
            [chocolatier.engine.events :as ev]
            [chocolatier.engine.rbush :as r]))


(def spatial-index-location
  [:state :spatial-index])

(defn entity-state->bounding-box
  "Format entity state for use with spatial index. Includes all
   collision component state in the metadata of the spatial tree item"
  [id {:keys [pos-x pos-y offset-x offset-y]} {:keys [height width attributes]}]
  (r/rbush-item pos-x
              pos-y
              ;; Adjust the width and height of the bounding box based
              ;; on where the entity will be next frame
              (+ pos-x width offset-x)
              (+ pos-y height offset-y)
              ;; Include the parent ID so we can pass information
              ;; about who created this collidable entity
              {:id id :attributes attributes}))

(defn update-spatial-index!
  "Returns a spatial index with all elegible entities inserted into it."
  [state index entity-ids]
  (let [collidable-states (ecs/get-all-component-state state :collidable)
        moveable-states (ecs/get-all-component-state state :moveable)]
    (loop [entities entity-ids
           items (array)]
      (let [entity-id (first entities)]
        (if (nil? entity-id)
          (r/rbush-bulk-insert! index items)
          (let [move-state (get moveable-states entity-id)
                collision-state (get collidable-states entity-id)]
            (when (not (and (nil? move-state) (nil? collision-state)))
              (.push items (entity-state->bounding-box entity-id
                                                       move-state
                                                       collision-state)))
            (recur (rest entities) items)))))))

(defn get-or-create-spatial-index
  [state max-entries]
  (get-in state spatial-index-location (r/rbush max-entries)))

(defn mk-broad-collision-system
  "Returns a system function that creates a spatial index with max-entries.
   Only operates on entities that have the :collidable and :moveable components"
  [max-entries]
  (fn [state]
    (let [entity-ids (ecs/entities-with-multi-components state [:collidable :moveable])
          spatial-index (get-or-create-spatial-index state max-entries)]
      ;; Clear the spatial index for the frame since we can't modify
      ;; it once it's been inserted
      (r/rbush-clear! spatial-index)
      (update-spatial-index! state spatial-index entity-ids)
      (assoc-in state spatial-index-location spatial-index))))

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

(defn self? [id item]
  (keyword-identical? id (:id (nth item 4))))

(defn self-attack? [id item]
  (keyword-identical? id (get-in (nth item 4) [:attributes :from-id])))

(defn collision-events
  "Returns a hashmap of all collision events by entity ID"
  [collision-items spatial-index width height]
  (loop [items collision-items
         accum (transient {})]
    (let [item (first items)]
      (if item
        (if (valid-collision-item? item width height)
          (let [id (:id (nth item 4))
                collisions (r/rbush-search spatial-index item)]
            (if (some #(not (or (self? id %) (self-attack? id %)))
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
          items (r/rbush-all spatial-index)
          events (collision-events items spatial-index width height)]
      (ev/batch-emit-events state [:collision] events))))
