(ns chocolatier.engine.systems.movement
  "System for handling entity movements"
  (:require [chocolatier.engine.ces :as ces]))


(defn movement-system
  [state fns entity-ids]
  (ces/iter-entities state fns entity-ids))
