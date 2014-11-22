(ns chocolatier.engine.components.ai
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]))

(defn include-player-and-renderable-state
  "Include the renderable component state for :player1"
  [state component-id entity-id]
  (let [renderable-state (ces/get-component-state state :renderable entity-id)
        player-state (ces/get-component-state state :renderable :player1)
        component-state (ces/get-component-state state component-id entity-id)
        inbox (ces/get-event-inbox state component-id entity-id)]
    [entity-id component-state renderable-state player-state component-id inbox]))

(defmulti behavior
  "Decides what the entity should do. Emits messages."
  (fn [entity-id component-state renderable-state player-state component-id inbox]
    entity-id))

(defmethod behavior :default
  [entity-id component-state renderable-state player-state component-id inbox]
  (let [{player-pos-x :pos-x player-pos-y :pos-y}  player-state
        {:keys [pos-x pos-y]} renderable-state
        event [:move-change entity-id {:offset-x (if (< player-pos-x pos-x) 1 -1)
                                       :offset-y (if (< player-pos-y pos-y) 1 -1)}]]
    ;; Go towards the x,y of the player
    [component-state [event]]))
