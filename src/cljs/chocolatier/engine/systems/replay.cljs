(ns chocolatier.engine.systems.replay
  (:require [chocolatier.engine.events :as ev]))


(defn replay-system
  "Returns a system function that makes a copy of the game state every n frames."
  [interval-n keep-copies-n]
  (fn [state]
    (let [{:keys [snapshot-counter snapshots]} (:game state)
          replay? (seq (ev/get-events state [:replay]))
          take-snapshot? (> (inc snapshot-counter) interval-n)
          counter (if ^boolean take-snapshot? 0 (inc snapshot-counter))
          interum-state (assoc-in state [:game :snapshot-counter] counter)
          update-fn (fn [snapshots]
                      (if ^boolean (> (inc (count snapshots)) keep-copies-n)
                        (rest (conj snapshots state))
                        (conj snapshots state)))
          updated-state (if ^boolean take-snapshot?
                          (update-in interum-state [:game :snapshots] update-fn)
                          interum-state)]
      (if replay?
        (or (first snapshots) updated-state)
        updated-state))))
