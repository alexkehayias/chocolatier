(ns chocolatier.entities.enemy
  (:require [chocolatier.utils.logging :refer [debug info warn error]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]
            [chocolatier.engine.components.animateable :refer [mk-animateable-state]]
            [chocolatier.engine.components.collidable :refer [mk-collidable-state]]
            [chocolatier.engine.components.moveable :refer [mk-moveable-state]]
            [chocolatier.engine.components.damage :refer [mk-damage-state]]))


(defn create-enemy!
  "Create a enemy by initializing some component state and adding the
   entity to state. Returns a function that takes a state hashmap."
  [state stage uid]
  (let [pos-x (* 1000 (js/Math.random))
        pos-y (* 1000 (js/Math.random))
        animation-state (mk-animateable-state stage
                                              "img/bunny.png"
                                              pos-x pos-y
                                              :stand-down
                                              [:stand-up 26 37 26 37 0 0 1]
                                              [:stand-down 26 37 26 37 0 0 1]
                                              [:stand-left 26 37 26 37 0 0 1]
                                              [:stand-right 26 37 26 37 0 0 1]
                                              [:hit-up 20 30 20 30 0 0 1])
        move-state (mk-moveable-state pos-x pos-y)
        collision-state (mk-collidable-state 26 37 nil)
        damage-state (mk-damage-state 200 5)]
    (ces/mk-entity state uid [[:moveable move-state]
                              [:animateable animation-state]
                              [:collidable collision-state]
                              [:damage damage-state]
                              :collision-debuggable
                              :ai])))
