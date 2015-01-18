(ns chocolatier.engine.systems.ai
  "System for handling entity artificial intelligence"
  (:require [chocolatier.engine.ces :as ces]
            [clojure.core.reducers :as r]))


(defn ai-system
  [state fns entity-ids]
  (let [player-state (ces/get-component-state state :moveable :player1)]
    (ces/iter-fns state (for [f fns, e entity-ids]
                          #(f % e {:player-state player-state})))))
