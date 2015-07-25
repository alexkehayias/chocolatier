(ns chocolatier.engine.components.moveable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))

(defn mk-moveable-state
  "Returns a hashmap of updated state with all required fields for the moveable
   component state"
  [pos-x pos-y]
  {:pos-x pos-x :pos-y pos-y})

(defn move
  "Check if there is an input-change, collision events, and calculates the
   new position of the entity on the screen."
  [entity-id component-state inbox]
  (let [{:keys [pos-x pos-y]} component-state
        collision? (seq (filter #(= (:event-id %) :collision) inbox))
        move-change (first (filter #(= (:event-id %) :move-change) inbox))
        {:keys [offset-x offset-y] :or {offset-x 0 offset-y 0}} (:msg move-change)
        new-pos-x (- pos-x offset-x)
        new-pos-y (- pos-y offset-y)
        updated-state (assoc component-state :pos-x new-pos-x :pos-y new-pos-y)]
    ;; If there WILL be a collision, don't emit a move otherwise emit
    ;; the intended movement
    (if collision?
      component-state
      (if move-change
        [updated-state
         [(ev/mk-event {:pos-x new-pos-x :pos-y new-pos-y} [:move entity-id])]]
        component-state))))
