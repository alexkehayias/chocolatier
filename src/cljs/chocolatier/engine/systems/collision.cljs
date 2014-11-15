(ns chocolatier.engine.systems.collision
  "System for rendering entities"
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]))


(defn collision-system
  [state fns entity-ids]
  (ces/iter-fns state (for [f fns, e entity-ids] #(f % e))))
