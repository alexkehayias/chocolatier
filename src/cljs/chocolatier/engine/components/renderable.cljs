(ns chocolatier.engine.components.renderable
  (:use [chocolatier.utils.logging :only [debug error]]))


(defn update-sprite
  "Update the entities sprite"
  [component-state entity-id]
  (let [sprite (:sprite component-state)]
    ;; Mutate the x and y position
    (set! (.-position.x sprite) (:pos-x component-state))
    (set! (.-position.y sprite) (:pos-x component-state))
    component-state))
