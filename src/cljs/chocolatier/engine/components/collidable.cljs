(ns chocolatier.engine.components.collidable
  (:require [chocolatier.engine.ces :as ces]))

(defn mk-collidable-state
  "Returns a hashmap of updated state with all required collision 
   component state"
  [state entity-id height width hit-radius]
  (ces/mk-component-state state :collidable entity-id
                          {:width width
                           :height height
                           :hit-radius hit-radius}))
