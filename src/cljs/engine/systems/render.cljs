(ns chocolatier.engine.systems.render
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Renderable]]
            [chocolatier.engine.components :as c]))


(defn render-system [state]
  (let [entities @(:entities state)
        tiles @(:tiles state)
        {:keys [stage renderer]} (-> state :game deref)]
    (doseq [entity entities]
      (when (satisfies? Renderable entity)
        (c/render entity stage)))
    (doseq [tile tiles]
      (when (satisfies? Renderable tile)
        (c/render tile stage)))
    (.render renderer stage)))
