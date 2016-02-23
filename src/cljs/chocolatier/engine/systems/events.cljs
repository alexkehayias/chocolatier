(ns chocolatier.engine.systems.events
  (:require [chocolatier.engine.events :as ev]))


(defn init-events-system
  "Adds an :events entry to the state hashmap."
  [state]
  (-> state
      (assoc-in ev/queue-path {})
      (assoc-in ev/subscription-path {})))

;; TODO manage subscriptions here too
(defn event-system
  "Clear out events queue. Returns update game state."
  [state]
  (ev/clear-events-queue state))
