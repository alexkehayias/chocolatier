(ns chocolatier.engine.systems.movement
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Moveable]]
            [chocolatier.engine.components :as c]))


(defn movement-system [state time]
  (swap! (:entities state)
         #(into % (for [[id entity] (seq %)] 
                    (if (satisfies? Moveable entity)
                      [id (c/move entity state)]
                      [id entity])))))
