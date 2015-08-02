(ns chocolatier.engine.systems.movement
  "System for handling entity movements"
  (:require [chocolatier.engine.ces :as ces]))


(defn movement-system
  [state f entity-ids]
  (reduce f state entity-ids))
