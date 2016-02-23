(ns chocolatier.engine.systems.combat
  "System for handling entity combat")


(defn attack-system
  [state f entity-ids]
  (reduce f state entity-ids))

(defn damage-system
  [state f entity-ids]
  (reduce f state entity-ids))
