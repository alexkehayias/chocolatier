(ns chocolatier.engine.systems.combat
  "System for handling entity combat"
  (:require [chocolatier.engine.ces :as ces]))


(defn attack-system
  [state f entity-ids]
  (reduce f state entity-ids))

(defn damage-system
  [state f entity-ids]
  (reduce f state entity-ids))
