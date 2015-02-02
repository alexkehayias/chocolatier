(ns chocolatier.engine.systems.debug
  (:require [chocolatier.engine.ces :as ces]))

(defn debug-collision-system
  "Adds debug information for any debuggable entity"
  [state fns entity-ids]
  (ces/iter-entities state fns entity-ids))
