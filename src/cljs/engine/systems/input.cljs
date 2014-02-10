(ns chocolatier.engine.systems.input
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [UserInput]]
            [chocolatier.engine.components :as c]))


(defn input-system
  "Calls all UserInput entities to react to user input"
  [state time]
  (swap! (:entities state)
         (fn [ents]
           (map #(when (satisfies? UserInput %)
                   (debug "react-to-user" (c/react-to-user-input % state time))
                   (c/react-to-user-input % state time))
                ents))))
