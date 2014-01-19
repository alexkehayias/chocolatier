(ns chocolatier.engine.systems.input
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.state :as s]
            [chocolatier.engine.components :refer [Controllable]]
            [chocolatier.engine.components :as c]))


(defn input-system
  "Calls all Controllable entities to react to user input"
  [state time]
  (doseq [entity @(:entities state)]
    (when (satisfies? Controllable entity)
      (c/react-to-user-input entity state time))))
