(ns chocolatier.engine.systems.ai
  "System for handling entity artificial intelligence"
  (:require [chocolatier.engine.ces :as ces]
            [clojure.core.reducers :as r]))


(defn ai-system
  [state f entity-ids]
  (let [player (ces/get-component-state state :moveable :player1)
        player-state {:player-state player}]
    (reduce #(f %1 %2 player-state) state entity-ids)))
