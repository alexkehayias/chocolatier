(ns chocolatier.engine.components.collidable
  (:require [chocolatier.engine.ces :as ces]))


(defn mk-collidable-state
  "Returns a hashmap of updated state with all required collision 
   component state"
  [width height hit-radius]
  {:width width
   :height height
   :hit-radius hit-radius})
