(ns chocolatier.engine.components.moveable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.events :as ev]))


(defn mk-moveable-state
  "Returns a hashmap of updated state with all required fields for the moveable
   component state.

   Args:
   - move-rate: how many pixels the entity will move in a single frame
   - direction: the direction of the movement"
  [move-rate direction]
  {:offset-x 0 :offset-y 0
   :move-rate move-rate
   :direction direction})

(defn collision-event? [inbox]
  (some #(when (keyword-identical? (:event-id %) :collision) %) inbox))

(defn get-move-change-event [inbox]
  (some #(when (keyword-identical? (:event-id %) :move-change) %) inbox))

(def direction->offset
  {:up [0 1]
   :down [0 -1]
   :left [1 0]
   :right [-1 0]
   :up-right [-1 1]
   :up-left [1 1]
   :down-right [-1 -1]
   :down-left [1 -1]
   nil [0 0]})

(def offset->direction
  {[0 1] :up
   [0 -1] :down
   [1 0] :left
   [-1 0] :right
   [-1 1] :up-right
   [1 1] :up-left
   [-1 -1] :down-right
   [1 -1] :down-left
   [0 0] nil})

(defn move
  "Check if there is an input-change, collision events, and calculates the
   new position of the entity on the screen."
  [entity-id component-state {:keys [inbox]}]
  (let [{:keys [move-rate direction]} component-state
        {new-direction :direction} (:msg (get-move-change-event inbox))
        collision? (collision-event? inbox)]
    (let [next-direction (or new-direction direction)
          [offset-x offset-y] (if collision?
                                [0 0]
                                (map #(* move-rate %)
                                     (get direction->offset next-direction)))]
      ;; If there is no offset then no need to send a message
      (if (and (zero? offset-x) (zero? offset-y))
        {:offset-x offset-x
         :offset-y offset-y
         :move-rate move-rate
         :direction next-direction})
      [{:offset-x offset-x
        :offset-y offset-y
        :move-rate move-rate
        :direction next-direction}
       [(ev/mk-event {:offset-x offset-x :offset-y offset-y}
                     [:position-change entity-id])]])))
