 (ns chocolatier.engine.systems.collision
  "System for checking collisions between entities"
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))

;; TODO Optimize this function it sucks
(defn get-multi-component-state
  "Returns a collection of hashmaps of component state. Append an :id field
   for the entity's unique ID"
  [state component-ids entity-ids]
  ;; For each entity, for each component
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
  (.search rbush item))

(defn rbush-all
  [rbush]
  (.all rbush))

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
  (let [items (array)]
    ;; YUK! This is to prevent having to call clj->js which is slow so
    ;; we will use side effects instead
    (doseq [e entity-states]
      (.push items (entity-state->bounding-box e)))
    (-> index
        (rbush-clear)
        (rbush-bulk-insert items))))

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

(def collision-queue-path
  (conj ev/queue-path :collision))

(defn mk-narrow-collision-system
  [height width]
  (fn [state]
    [state]
    (let [ ;; Get only the entities that are collidable and moveable
          spatial-index (get-in state spatial-index-location)
          component-ids [:collidable :moveable]
          ;; Use the spatial index for the collection of items to check
          ;; Remove any that are not in the viewport
          items (filter (fn [[x y _ _ {:keys [offset-x offset-y]}]]
                          (and (< x width) (< y height)))
                        (rbush-all spatial-index))
          ;; TODO skip the combinations already checked
          event-pairs (for [i items
                            :let [id (:id (last i))
                                  collisions (rbush-search spatial-index i)]
                            :when (some #(not= id (:id (last %))) collisions)]
                        [id [(ev/mk-event {:colliding? true} [:collision id])]])]
      ;; TODO check if the entity is already colliding and skip sending
      ;; an event
      (ev/batch-emit-events state [:collision] (into {} event-pairs)))))
