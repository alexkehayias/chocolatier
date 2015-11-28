(ns chocolatier.engine.systems.ttl
  "System for handling entities with a limited life time"
  (:require [chocolatier.engine.ces :as ces]))


(defn ttl-system
  [state f entity-ids]
  (reduce f state entity-ids))
