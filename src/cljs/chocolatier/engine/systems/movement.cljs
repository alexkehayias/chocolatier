(ns chocolatier.engine.systems.movement
  "System for handling entity movements")


(defn movement-system
  [state f entity-ids]
  (reduce f state entity-ids))
