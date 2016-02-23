(ns chocolatier.engine.systems.user-input
  "System for reacting to user input")


(defn user-input-system
  "Call all the functions for reacting to user input"
  [state f entity-ids]
  (reduce f state entity-ids))
