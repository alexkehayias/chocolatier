(ns chocolatier.engine.systems.debug
  (:require [chocolatier.engine.ces :as ces]
            [chocolatier.utils.logging :as log]))

(defn debug-system
  "Adds debug information for any debuggable entity"
  [state fns entity-ids]
  (ces/iter-fns state (for [f fns, e entity-ids] #(f % e))))
