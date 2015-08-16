(ns chocolatier.engine.components.moveable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))


(defn mk-moveable-state
  "Returns a hashmap of updated state with all required fields for the moveable
   component state"
  [pos-x pos-y]
  {:pos-x pos-x :pos-y pos-y :offset-x 0 :offset-y 0})

(defn collision-event? [inbox]
  (first (filter #(= (:event-id %) :collision) inbox)))

(defn get-move-change-event [inbox]
  (first (filter #(= (:event-id %) :move-change) inbox)))

(defn get-position
  [movement-event ^boolean collision?
   last-pos-x last-pos-y
   last-offset-x last-offset-y]
  (let [[offset-x offset-y] (if collision?
                              [0 0]     ; No movement
                              (if movement-event
                                (let [{:keys [offset-x offset-y]}
                                      (:msg movement-event)]
                                  [offset-x offset-y])
                                [last-offset-x last-offset-y]))]
    {:pos-x (- last-pos-x offset-x)
     :pos-y (- last-pos-y offset-y)
     :offset-x offset-x
     :offset-y offset-y}))

(defn move
  "Check if there is an input-change, collision events, and calculates the
   new position of the entity on the screen."
  [entity-id component-state inbox]
  (let [{:keys [pos-x pos-y offset-x offset-y]} component-state
        movement-event (get-move-change-event inbox)
        collision? (collision-event? inbox)]
    (get-position movement-event
                  collision?
                  pos-x pos-y
                  offset-x
                  offset-y)))
