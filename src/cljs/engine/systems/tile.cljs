(ns chocolatier.engine.systems.tile
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.state :as s]
            [chocolatier.engine.components :refer [Tile]]
            [chocolatier.engine.components :as c]))


(defn tile-system [state time]
  (doseq [tile (-> state deref :tile-map :tiles)]
    ;; TODO only move if the user is on a tile that is traversable
    ;; (when (satisfies? Tile tile)
    ;;   (c/move-by-offset tile state))
    nil))
