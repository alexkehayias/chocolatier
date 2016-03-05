(ns chocolatier.engine.components.ai
  (:require [chocolatier.engine.events :as ev]
            [chocolatier.engine.components.moveable :refer [offset->direction]]))

(defn behavior
  [entity-id component-state {:keys [moveable moveable-player1]}]
  (let [{player-pos-x :pos-x player-pos-y :pos-y} moveable-player1
        {:keys [pos-x pos-y]} moveable
        offset [(if ^boolean (< player-pos-x pos-x) 1 -1)
                (if ^boolean (< player-pos-y pos-y) 1 -1)]
        event (ev/mk-event {:direction (get offset->direction offset)}
                           [:move-change entity-id])]
    [component-state [event]]))
