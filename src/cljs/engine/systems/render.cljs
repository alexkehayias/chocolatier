(ns chocolatier.engine.systems.render
  "System for rendering entities"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Renderable]]
            [chocolatier.engine.components :as c]))


(defn render-system [state]
  (let [entities (:entities state)
        tiles (:tiles state)
        {:keys [stage renderer]} (-> state :game deref)]
    ;; Render changes to entities
    (swap! entities (fn [ents]
                       (map #(when (satisfies? Renderable %)
                               (c/render % stage))
                            ents)))
    ;; Render changes to tiles
    (swap! tiles (fn [ts]
                   (map #(when (satisfies? Renderable %)
                           (c/render % stage))
                        ts)))
    ;; Render to the stage
    (.render renderer stage)))
