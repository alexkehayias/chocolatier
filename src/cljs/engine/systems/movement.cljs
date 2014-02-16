(ns chocolatier.engine.systems.movement
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Moveable]]
            [chocolatier.engine.components :as c]))


(defn movement-system [state time]
  (swap! (:entities state)
         (fn [ents]
           (map #(when (satisfies? Moveable %) (c/move %)) ents))))
