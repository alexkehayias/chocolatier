(ns chocolatier.engine.systems.tick
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.state :as s]
            [chocolatier.engine.components :refer [Entity]]
            [chocolatier.engine.components :as c]))


(defn tick-system [state time]
  (swap! (:entities state)
         (fn [ents]
           (map #(if (satisfies? Entity %)
                   (c/tick %)
                   %)
                ents))))
