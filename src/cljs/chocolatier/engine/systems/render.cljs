(ns chocolatier.engine.systems.render
  "System for rendering entities"
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]))


(defn render-system
  "Mutates all sprites then renders the stage in one shot. 
   Returns updated state."
  [state fns entity-ids]
  (let [{:keys [renderer stage]} (-> state :game :rendering-engine)
        render-state (for [f fns, e entity-ids]
                       (f state e))
        updated-state (apply ces/deep-merge render-state)]
    (.render renderer stage)
    updated-state))
