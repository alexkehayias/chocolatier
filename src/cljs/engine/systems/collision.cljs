(ns chocolatier.engine.systems.collision
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.state :as s]
            [chocolatier.engine.components :refer [Collidable]]
            [chocolatier.engine.components :as c]))


(defn collision-system
  "Calls all UserCollision entities to react to user collision"
  [state time]
  (doseq [entity @(:entities state)]
    (if (satisfies? Collidable entity)
      (c/check-collision entity state time)
      entity)))
