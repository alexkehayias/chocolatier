(ns chocolatier.engine.systems.text)


(defn text-system
  [state f entity-ids]
  (reduce f state entity-ids))
