(ns chocolatier.engine.components.moveable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]))


(defn include-renderable-state
  "Include the renderable component state for the entity-id"
  [state component-id entity-id]
  (let [renderable-state (ces/get-component-state state :renderable entity-id)
        component-state (ces/get-component-state state component-id entity-id)
        inbox (ces/get-event-inbox state component-id entity-id)]
    [entity-id component-state renderable-state component-id inbox]))

(defmulti move
  "Determine the movement of the entity in screen coordinates.
   Emits an event of :move with a hashmap of move-x move-y."
  (fn [entity-id component-state renderable-state component-id inbox] entity-id))

(defmethod move :default
  [entity-id component-state renderable-state component-id inbox]
  ;; Check if there is an input-change, collision events
  (let [collision? (seq (filter #(= (:event-id %) :collision) inbox))
        input-change (first (filter #(= (:event-id %) :move-change) inbox)) 
        {:keys [offset-x offset-y] :or {offset-x 0 offset-y 0}} (:msg input-change)]
    ;; If there WILL be a collision, don't emit a move otherwise emit
    ;; the intended movement
    (if collision?
      component-state
      (if input-change
        [component-state [[:move entity-id {:move-x offset-x :move-y offset-y}]]] 
        component-state))))
