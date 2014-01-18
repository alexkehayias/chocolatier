(ns chocolatier.engine.systems.render
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Renderable]]
            [chocolatier.engine.components :as c]))


(defn render-system [state time]
  (let [entities @(:entities state)
        stage (-> state :game deref :stage)
        renderer (-> state :game deref :renderer)]
    (doseq [entity entities]
      (when (satisfies? Renderable entity)
        (c/render entity stage)))
    (.render renderer stage)))
