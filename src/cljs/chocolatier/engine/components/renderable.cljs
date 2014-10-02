(ns chocolatier.engine.components.renderable
  (:use [chocolatier.utils.logging :only [debug error]]))


(defn update-sprite
  "Update the entities sprite"
  [component-state entity-id]
  (let [sprite (:sprite component-state)]
    ;; Mutate the x and y position
    (set! (.-position.x sprite) 25)
    (set! (.-position.y sprite) 25)
    component-state))
