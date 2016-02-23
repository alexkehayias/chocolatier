(ns chocolatier.engine.systems.debug)


(defn debug-collision-system
  "Adds debug information for any debuggable entity"
  [state f entity-ids]
  (reduce f state entity-ids))
