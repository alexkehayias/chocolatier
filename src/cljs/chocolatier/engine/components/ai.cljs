(ns chocolatier.engine.components.ai
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))

(defn include-player-and-renderable-state
  "Include the renderable component state for :player1"
  [state component-id entity-id]
  (let [renderable-state (ces/get-component-state state :renderable entity-id)
        component-state (ces/get-component-state state component-id entity-id)]
    [entity-id component-state renderable-state]))

(defn behavior
  [entity-id component-state renderable-state {:keys [player-state]}]
  (let [{player-pos-x :pos-x player-pos-y :pos-y} player-state
        {:keys [pos-x pos-y]} renderable-state
        msg {:offset-x (if (< player-pos-x pos-x) 1 -1)
             :offset-y (if (< player-pos-y pos-y) 1 -1)}
        event (ev/mk-event msg :move-change entity-id)]
    ;; Go towards the x,y of the player
    [component-state [event]]))
