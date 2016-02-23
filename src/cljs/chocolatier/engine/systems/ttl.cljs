(ns chocolatier.engine.systems.ttl
  "System for handling entities with a limited life time")


(defn ttl-system
  [state f entity-ids]
  (reduce f state entity-ids))
