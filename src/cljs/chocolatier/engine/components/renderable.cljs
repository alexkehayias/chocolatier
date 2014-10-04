(ns chocolatier.engine.components.renderable
  (:use [chocolatier.utils.logging :only [debug error]]))


(defn default-update-sprite
  "Update the entities sprite"
  [component-state entity-id]
  (let [sprite (:sprite component-state)]
    ;; Mutate the x and y position
    (set! (.-position.x sprite) (:pos-x component-state))
    (set! (.-position.y sprite) (:pos-y component-state))
    component-state))

(defmulti update-sprite
  (fn [component-state entity-id] entity-id))

(defmethod update-sprite :default
  [component-state entity-id]
  (debug (.-position.x (:sprite component-state)))
  default-update-sprite)

(defmethod update-sprite :player1
  [component-state entity-id]
  (let [sprite (:sprite component-state)]
    ;; Mutate the x and y position
    (set! (.-position.x sprite) (:pos-x component-state))
    (set! (.-position.y sprite) (:pos-x component-state))
    (if (< (:pos-x component-state) 50)
      (assoc component-state :pos-x (+ 1 (:pos-x component-state)))
      (assoc component-state :pos-x (- (:pos-x component-state) 50)))))
