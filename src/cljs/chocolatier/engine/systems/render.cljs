(ns chocolatier.engine.systems.render
  "System for rendering entities"
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]))


(defn render-system
  "Mutates all sprites then renders the stage. Returns updated state."
  [state fns entity-ids]
  (let [{:keys [renderer stage]} (-> state :game :rendering-engine)
        ;; WARNING this is stateful since sprites are objects and are
        ;; being altered by component functions
        updated-state (ces/iter-fns state (for [f fns, e entity-ids] #(f % e)))]
    (.render renderer stage)
    updated-state))
