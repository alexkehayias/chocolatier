(ns chocolatier.entities.player
  (:require [chocolatier.utils.logging :refer [debug info warn error]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]
            [chocolatier.engine.components.animateable :refer [mk-animateable-state]]
            [chocolatier.engine.components.collidable :refer [mk-collidable-state]]
            [chocolatier.engine.components.moveable :refer [mk-moveable-state]]
            [chocolatier.engine.components.attack :refer [mk-attack-state]]
            [chocolatier.engine.components.damage :refer [mk-damage-state]]))


(defn create-player!
  "Create a player by initializing some component state and adding the
   entity to state. Returns a function that takes a state hashmap."
  [stage uid pos-x pos-y map-x map-y]
  (fn [state]
    (info "Creating player" pos-x pos-y map-x map-y)
    (let [animation-state (mk-animateable-state stage
                                                "img/test_spritesheet.png"
                                                pos-x pos-y
                                                :stand-down
                                                [:stand-up 832 1344 64 64 8 0 1]
                                                [:stand-down 832 1344 64 64 10 0 1]
                                                [:stand-left 832 1344 64 64 9 0 1]
                                                [:stand-right 832 1344 64 64 11 0 1]
                                                [:walk-up 832 1344 64 64 8 0 9]
                                                [:walk-down 832 1344 64 64 10 0 9]
                                                [:walk-left 832 1344 64 64 9 0 9]
                                                [:walk-right 832 1344 64 64 11 0 9]
                                                [:attack-up 832 1344 64 64 4 0 8]
                                                [:attack-down 832 1344 64 64 6 0 8]
                                                [:attack-left 832 1344 64 64 5 0 8]
                                                [:attack-right 832 1344 64 64 7 0 8])
          move-state (mk-moveable-state pos-x pos-y)
          collision-state (mk-collidable-state 64 64 nil)
          attack-state (mk-attack-state nil)
          damage-state (mk-damage-state 100 10)]
      (ces/mk-entity state
                     uid
                     [[:animateable animation-state]
                      :controllable
                      [:collidable collision-state]
                      :collision-debuggable
                      [:moveable move-state]
                      [:attack attack-state]
                      [:damage damage-state]]))))
