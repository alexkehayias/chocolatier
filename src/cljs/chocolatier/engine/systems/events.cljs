(ns chocolatier.engine.systems.events
  (:require [chocolatier.engine.events :as ev]))


(defn init-events-system
  "Adds an :events entry to the state hashmap."
  [state]
  (assoc-in state ev/queue-path {}))

(defn event-system
  "Clear out events queue. Returns update game state."
  [state]
  (ev/clear-events-queue state))
