 (ns chocolatier.engine.systems.collision
  "System for checking collisions between entities"
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))


(defn get-multi-component-state
  "Returns a collection of hashmaps of component state. Append an :id field
   for the entity's unique ID"
  [state component-ids entity-ids]
  (map
   (fn [id]
     (into {:id id} (map #(ces/get-component-state state % id) component-ids)))
   entity-ids))

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
  (js->clj (.search rbush item)))

(defn rbush-all
  [rbush]
  (js->clj (.all rbush)))

(defn entity-state->bounding-box
  "Format entity state for use with spatial index"
  [{:keys [pos-x pos-y height width id]}]
  (rbush-item pos-x
              pos-y
              (+ pos-x width)
              (+ pos-y height)
              {:id id}))

(defn update-spatial-index
  [index entity-states]
  (-> index
      (rbush-clear)
      (rbush-bulk-insert (clj->js (map entity-state->bounding-box entity-states)))))

(def spatial-index-location
  [:state :spatial-index])

(defn mk-broad-collision-system
  [max-entries]
  (fn [state]
    (let [;; Get only the entities that are collidable and moveable
          component-ids [:collidable :moveable]
          entity-ids (ces/entities-with-multi-components (:entities state)
                                                         component-ids)
          entity-states (get-multi-component-state state component-ids entity-ids)
          ;; Get or create the spatial index
          spatial-index (get-in state spatial-index-location (rbush max-entries))]
      (assoc-in state spatial-index-location
                (update-spatial-index spatial-index entity-states)))))

(defn narrow-collision-system
  [state]
  (let [ ;; Get only the entities that are collidable and moveable
        spatial-index (get-in state spatial-index-location)
        component-ids [:collidable :moveable]
        entity-ids (ces/entities-with-multi-components (:entities state)
                                                       component-ids)
        entity-states (get-multi-component-state state component-ids entity-ids)
        events (for [e entity-states
                     :let [collisions (rbush-search spatial-index
                                                    (entity-state->bounding-box e))]
                     :when (some #(not= (:id e) (:id (last %))) collisions)]
                 (ev/mk-event {:colliding? true} [:collision (:id e)]))]
    (ev/emit-events state events)))
