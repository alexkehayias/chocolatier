(ns chocolatier.engine.systems.tile
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.state :as s]))


(defn tile-system [state time]
  (doseq [tile @(:tiles state)]
    nil
))
