(ns chocolatier.engine.components.renderable
  (:use [chocolatier.utils.logging :only [debug error]]))


(defmulti update-sprite
  (fn [entity-id component-state inbox] entity-id))

(defmethod update-sprite :default
  [entity-id component-state inbox]
  (let [sprite (:sprite component-state)]
    ;; Mutate the x and y position
    (set! (.-position.x sprite) (:pos-x component-state))
    (set! (.-position.y sprite) (:pos-y component-state))
    component-state))

(defmethod update-sprite :player1
  [entity-id component-state inbox]
  (let [sprite (:sprite component-state)
        {:keys [pos-x pos-y offset-x offset-y]} component-state
        updated-state (assoc component-state :pos-x (- pos-x offset-x )
                             :pos-y (- pos-y offset-y))]
    ;; Mutate the x and y position
    (set! (.-position.x sprite) (:pos-x updated-state))
    (set! (.-position.y sprite) (:pos-y updated-state))
    updated-state))
