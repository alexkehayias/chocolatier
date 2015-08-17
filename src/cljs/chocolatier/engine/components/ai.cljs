(ns chocolatier.engine.components.ai
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))


(defn include-player-state
  "Include the moveable component state for :player1"
  [state component-id entity-id]
  {:moveable-state (ces/get-component-state state :moveable entity-id)})

(defn behavior
  [entity-id component-state
   {:keys [moveable-state
           player-state                 ; passed in from system
           ]}]
  (let [{:keys [last-player-pos-x last-player-pos-y]} component-state
        {player-pos-x :pos-x player-pos-y :pos-y} player-state]
    (if (and (= player-pos-x last-player-pos-x)
             (= player-pos-y last-player-pos-y))
      component-state
      (let [{:keys [pos-x pos-y]} moveable-state
            msg {:offset-x (if (< player-pos-x pos-x) 1 -1)
                 :offset-y (if (< player-pos-y pos-y) 1 -1)}
            event (ev/mk-event msg [:move-change entity-id])]
        ;; Go towards the x,y of the player
        [{:last-player-pos-x player-pos-x :last-player-pos-y player-pos-y}
         [event]]))))
