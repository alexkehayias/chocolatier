(ns chocolatier.engine.components.ai
  (:require [chocolatier.engine.events :as ev]
            [chocolatier.engine.components.moveable :refer [offset->direction]]))

(defn behavior
  [entity-id component-state {:keys [position position-player1]}]
  (let [{player-screen-x :screen-x player-screen-y :screen-y} position-player1
        {:keys [screen-x screen-y]} position
        offset [(if ^boolean (< player-screen-x screen-x) 1 -1)
                (if ^boolean (< player-screen-y screen-y) 1 -1)]
        event (ev/mk-event {:direction (get offset->direction offset)}
                           [:move-change entity-id])]
    [component-state [event]]))
