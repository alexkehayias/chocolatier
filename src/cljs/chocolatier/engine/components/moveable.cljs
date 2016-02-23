(ns chocolatier.engine.components.moveable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.events :as ev]))


(defn mk-moveable-state
  "Returns a hashmap of updated state with all required fields for the moveable
   component state.

   Args:
   - pos-x: the x coordinate of the screen position
   - pos-y: the y coordinate of the screen position
   - move-rate: how many pixels the entity will move in a single frame
   - direction: the direction of the movement"
  [pos-x pos-y move-rate direction]
  {:pos-x pos-x :pos-y pos-y
   :offset-x 0 :offset-y 0
   :move-rate move-rate
   :direction direction})

(defn collision-event? [inbox]
  (some #(when (= (:event-id %) :collision) %) inbox))

(defn get-move-change-event [inbox]
  (some #(when (= (:event-id %) :move-change) %) inbox))

(def direction->offset
  {:up [0 1]
   :down [0 -1]
   :left [1 0]
   :right [-1 0]
   :up-right [-1 1]
   :up-left [1 1]
   :down-right [-1 -1]
   :down-left [1 -1]
   :none [0 0]})

(def offset->direction
  {[0 1] :up
   [0 -1] :down
   [1 0] :left
   [-1 0] :right
   [-1 1] :up-right
   [1 1] :up-left
   [-1 -1] :down-right
   [1 -1] :down-left
   [0 0] :none})

(defn get-position
  [{:keys [direction ^boolean stop?] :as movement-event}
   move-rate
   ^boolean collision?
   pos-x pos-y
   last-direction]
  (let [direction (or direction last-direction)
        [offset-x offset-y] (if collision?
                              [0 0]
                              (map #(* move-rate %) (direction->offset direction)))]
    {:pos-x (- pos-x offset-x)
     :pos-y (- pos-y offset-y)
     :offset-x offset-x
     :offset-y offset-y
     :move-rate move-rate
     :direction direction}))

(defn move
  "Check if there is an input-change, collision events, and calculates the
   new position of the entity on the screen."
  [entity-id component-state {:keys [inbox]}]
  (let [{:keys [pos-x pos-y move-rate direction]} component-state
        movement-event (:msg (get-move-change-event inbox))
        collision? (collision-event? inbox)]
    (get-position movement-event
                  move-rate
                  collision?
                  pos-x pos-y
                  direction)))
