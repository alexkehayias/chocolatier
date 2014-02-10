(ns chocolatier.engine.systems.tile
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :as c]))


(defn tile-system [state time]
  (c/move-layer @(:tile-map state) state))
