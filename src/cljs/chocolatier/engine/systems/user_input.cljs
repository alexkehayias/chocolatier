(ns chocolatier.engine.systems.user-input
  "System for reacting to user input"
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]))


(defn user-input-system
  "Call all the functions for reacting to user input"
  [state fns entity-ids]
  (ces/iter-entities state fns entity-ids))
