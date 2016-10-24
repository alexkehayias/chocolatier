(ns chocolatier.engine.systems.render
  "System for rendering entities"
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.pixi :as pixi]))


(defn sort-by-zindex [a b]
  (cond
    (< (.-position.z a) (.-position.z b)) -1
    (> (.-position.z a) (.-position.z b)) 1
    :else 0))

(defn render-system
  "Renders all the changes to sprites and other Pixi objects.
   Draws sprites in order of their zindex."
  [state]
  (let [{:keys [renderer stage]} (-> state :game :rendering-engine)]
    (.sort (.-children stage) sort-by-zindex)
    (pixi/render! renderer stage)
    state))
