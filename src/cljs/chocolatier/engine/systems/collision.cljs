(ns chocolatier.engine.systems.collision
  "System for checking collisions between entities"
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ecs :as ecs]
            [chocolatier.engine.events :as ev]
            [chocolatier.engine.rtree :as r]))


(def tilemap-spatial-index-location
  [:state :spatial-index-map])

(def entity-spatial-index-location
  [:state :spatial-index-entity])

(defn entity-state->bounding-box
  "Format entity state for use with spatial index. Represents where the entity
   will be.

   Args:
   - id: The unique ID of the entity
   - move-state: The moveable state of the entity
   - collision-state: The collideable state of the entity"
  [entity-id
   {:keys [pos-x pos-y offset-x offset-y]}
   {:keys [height width attributes]}]
  (r/rtree-item (+ pos-x offset-x)
                (+ pos-y offset-y)
                ;; Adjust the width and height of the bounding box based
                ;; on where the entity will be next frame
                (+ pos-x width offset-x)
                (+ pos-y height offset-y)
                ;; Include the parent ID so we can pass information
                ;; about who created this collidable entity
                {:id entity-id :attributes attributes}))

(defn index-entities!
  "Returns a spatial index with all elegible entities inserted."
  [state index entity-ids]
  ;; Always clear out before inserting items to avoid duplication
  (r/rtree-clear! index)
  (let [collidable-states (ecs/get-all-component-state state :collidable)
        moveable-states (ecs/get-all-component-state state :moveable)]
    (loop [entities entity-ids
           items (array)]
      (let [entity-id (first entities)]
        (if (nil? entity-id)
          (r/rtree-bulk-insert! index items)
          (let [move-state (get moveable-states entity-id)
                collision-state (get collidable-states entity-id)]
            (when (not (and (nil? move-state) (nil? collision-state)))
              (.push items (entity-state->bounding-box entity-id
                                                       move-state
                                                       collision-state)))
            (recur (rest entities) items)))))))

(defn get-or-create-entity-spatial-index
  [state max-entries]
  (get-in state entity-spatial-index-location (r/rtree max-entries)))

(def collision-queue-path
  (conj ev/queue-path :collision))

(defn attack?
  "Returns a boolean of whether the id is an attack"
  [attributes]
  (contains? attributes :damage))

(defn valid-entity-collision-item?
  "Returns true if the spatial index item, a four element js
   array, is valid for checking collisions against.

   Excludes:
   - Items that are not in the view port (via width and height)
   - Items that are an attack (attacks shouldn't collide with attacks)
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

(defn entity-collision-events
  "Returns a hashmap of all collision events by entity ID"
  [collision-items spatial-index width height]
  (loop [items collision-items
         accum (transient {})]
    (let [item (first items)]
      (if item
        (if (valid-entity-collision-item? item width height)
          (let [id (:id (nth item 4))
                collisions (r/rtree-search spatial-index item)]
            (if (some #(not (or (self? id %) (self-attack? id %)))
                      collisions)
              (recur (rest items)
                     (assoc! accum id [(ev/mk-event {:collisions collisions}
                                                    [:collision id])]))
              (recur (rest items) accum)))
          (recur (rest items) accum))
        (persistent! accum)))))

(defn mk-entity-collision-system
  "Returns a function parameterized by the height, width, of the view port and
   max entries for the R-tree.

   Returns an update game state with collision events emitted for all eligible
   entities stored in the spatial index"
  [height width max-entries]
  (fn [state]
    (let [entity-ids (ecs/entities-with-multi-components state [:collidable :moveable])
          spatial-index (get-or-create-entity-spatial-index state max-entries)
          items (r/rtree-all spatial-index)
          events (entity-collision-events items spatial-index width height)
          next-state (assoc-in state entity-spatial-index-location spatial-index)]
      (index-entities! state spatial-index entity-ids)
      (ev/batch-emit-events next-state [:collision] events))))

(defn get-or-create-tilemap-spatial-index
  [state max-entries]
  (get-in state tilemap-spatial-index-location (r/rtree max-entries)))

(defn index-tilemap!
  "Returns the index with all tiles in the map inserted"
  [state index tiles]
  (loop [tiles tiles
         items (array)]
    (let [tile (first tiles)]
      (if (nil? tile)
        (r/rtree-bulk-insert! index items)
        (let [{:keys [screen-x screen-y width height attributes]} tile]
          (when (:impassable? attributes)
            (.push items (r/rtree-item screen-x screen-y
                                       (+ screen-x width)
                                       (+ screen-y height)
                                       attributes)))
          (recur (rest tiles) items))))))

(defn tilemap-collision-events
  "Returns a hashmap of all collision events with the tilemap"
  [index moveable-states collidable-states entity-ids]
  (loop [entities entity-ids
         accum (transient {})]
    (let [entity-id (first entities)]
      (if (nil? entity-id)
        (persistent! accum)
        (let [move-state (get moveable-states entity-id)
              collision-state (get collidable-states entity-id)
              item (entity-state->bounding-box entity-id
                                               move-state
                                               collision-state)
              collisions (r/rtree-search index item)]
          (if (seq collisions)
            (recur (rest entities)
                   (assoc! accum entity-id
                           [(ev/mk-event {:collisions collisions}
                                         [:collision entity-id])]))
            (recur (rest entities) accum)))))))

(defn mk-tilemap-collision-system
  [height width max-entries]
  ;; HACK for now since tilemaps are static, only index it once
  (let [refresh? (atom true)]
    (fn [state]
      (let [entity-ids (ecs/entities-with-multi-components state [:collidable :moveable])
            tiles (get-in state [:state :tiles])
            spatial-index (get-or-create-tilemap-spatial-index state max-entries)
            spatial-index (if @refresh?
                            (do (reset! refresh? false)
                                (index-tilemap! state spatial-index tiles))
                            spatial-index)
            moveable-states (ecs/get-all-component-state state :moveable)
            collidable-states (ecs/get-all-component-state state :collidable)
            events (tilemap-collision-events spatial-index
                                             moveable-states
                                             collidable-states
                                             entity-ids)]

        (-> state
            (assoc-in tilemap-spatial-index-location spatial-index)
            ;; Emit all of the collision events in one shot
            ;; FIX will this overwrite all collision events?
            (ev/batch-emit-events [:collision] events))))))
