(ns chocolatier.engine.systems.tile
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.state :as s]
            [chocolatier.engine.components :refer [Renderable]]
            [chocolatier.engine.components :as c]))


(defn tile-system [state time]
  (doseq [tile @(:tiles state)]
    (when (satisfies? Renderable)
      (c/render tile state))))
