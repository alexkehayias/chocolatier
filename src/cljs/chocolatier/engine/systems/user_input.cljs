(ns chocolatier.engine.systems.user-input
  "System for reacting to user input"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]))


(defn user-input-system
  "Call all the functions for reacting to user input"
  [state fns entity-ids]
  (log/debug "user-input-system: running" (count fns)
             "functions on" (count entity-ids) "entities")
  (let [updated-state (for [f fns, e entity-ids]
                        (f state e))]
    (log/debug "user-input-system:" updated-state)
    (apply ces/deep-merge updated-state)))
