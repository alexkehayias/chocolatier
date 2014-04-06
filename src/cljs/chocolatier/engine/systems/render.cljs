(ns chocolatier.engine.systems.render
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Renderable]]
            [chocolatier.engine.components :as c]))


(defn render-system [state]
  (let [entities (:entities state)
        tile-map (:tile-map state)
        {:keys [stage renderer]} (-> state :game deref)]
    ;; Render tile map changes
    ;; tile-map may be an empty hash
    (when (satisfies? Renderable @tile-map)
      (swap! tile-map #(c/render % state)))    
    ;; Render changes to entities
    (swap! entities
         #(into % (for [[id entity] (seq %)] 
                    (if (satisfies? Renderable entity)
                      [id (c/render entity stage)]
                      [id entity]))))
    ;; Render to the stage
    (.render renderer stage)))
