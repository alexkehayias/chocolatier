(ns chocolatier.entities.player
  (:require [chocolatier.utils.logging :refer [debug info warn error]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]
            [chocolatier.engine.components.animateable :refer [mk-animateable-state]]
            [chocolatier.engine.components.collidable :refer [mk-collidable-state]]
            [chocolatier.engine.components.moveable :refer [mk-moveable-state]]
            ))


(defn create-player!
  "Create a player by initializing some component state and adding the 
   entity to state. Returns a function that takes a state hashmap."
  [stage uid pos-x pos-y map-x map-y hit-radius]
  (fn [state]
    (info "Creating player" pos-x pos-y map-x map-y hit-radius)
    (-> state
        (mk-animateable-state stage
                              uid
                              "static/images/test_spritesheet.png"
                              pos-x pos-y
                              :standing
                              [:standing 832 1344 64 64 10 0 1]
                              [:walk-up 832 1344 64 64 8 0 9]
                              [:walk-down 832 1344 64 64 10 0 9]
                              [:walk-left 832 1344 64 64 9 0 9]
                              [:walk-right 832 1344 64 64 11 0 9])
        (mk-moveable-state uid pos-x pos-y)
        (mk-collidable-state uid 64 64 hit-radius)
        (ces/mk-entity uid [:animateable
                            :controllable
                            :collidable
                            :collision-debuggable
                            :moveable])
        (ev/subscribe uid :move-change uid)
        (ev/subscribe uid :action uid)
        (ev/subscribe uid :collision uid)
        (ev/subscribe uid :move uid))))
