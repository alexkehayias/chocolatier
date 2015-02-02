(ns chocolatier.engine.systems.ai
  "System for handling entity artificial intelligence"
  (:require [chocolatier.engine.ces :as ces]
            [clojure.core.reducers :as r]))


(defn ai-system
  [state fns entity-ids]
  (let [player (ces/get-component-state state :moveable :player1)
        player-state {:player-state player}]
    (ces/iter-entities state fns entity-ids player-state)))
