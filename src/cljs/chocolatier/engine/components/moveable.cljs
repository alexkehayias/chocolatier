(ns chocolatier.engine.components.moveable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))


(defn include-renderable-state
  "Include the renderable component state for the entity-id"
  [state component-id entity-id]
  (let [renderable-state (ces/get-component-state state :renderable entity-id)
        component-state (ces/get-component-state state component-id entity-id)
        inbox (ev/get-subscribed-events state entity-id)]
    [entity-id component-state renderable-state component-id inbox]))

(defn move
  [entity-id component-state renderable-state component-id inbox]
  ;; Check if there is an input-change, collision events
  (let [collision? (seq (filter #(= (:event-id %) :collision) inbox))
        move-change (first (filter #(= (:event-id %) :move-change) inbox)) 
        {:keys [offset-x offset-y] :or {offset-x 0 offset-y 0}} (:msg move-change)]
    ;; If there WILL be a collision, don't emit a move otherwise emit
    ;; the intended movement
    (if collision?
      component-state
      (if move-change
        [component-state [(ev/mk-event {:move-x offset-x :move-y offset-y}
                                       :move
                                       entity-id)]] 
        component-state))))
