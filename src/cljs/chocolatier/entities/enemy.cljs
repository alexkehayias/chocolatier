(ns chocolatier.entities.enemy
  (:require [chocolatier.utils.logging :refer [debug info warn error]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.events :as ev]
            [chocolatier.engine.components.renderable :refer [mk-sprite-state
                                                              mk-text-sprite-state]]
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
        sprite-state (mk-sprite-state stage "/img/bunny.png")
        animation-state (mk-animateable-state :stand-down
                                              [:stand-up 26 37 26 37 0 0 1]
                                              [:stand-down 26 37 26 37 0 0 1]
                                              [:stand-left 26 37 26 37 0 0 1]
                                              [:stand-right 26 37 26 37 0 0 1]
                                              [:hit-up 20 30 20 30 0 0 1])
        move-state (mk-moveable-state pos-x pos-y 4 :down)
        collision-state (mk-collidable-state 26 37 nil)
        damage-state (mk-damage-state 50 5 5
                                      #(mk-text-sprite-state stage % {"font" "bold 12px Arial"
                                                               "fill" "red"
                                                               "stroke" "white"
                                                               "strokeThickness" 3}))]
    (ces/mk-entity state uid [[:moveable move-state]
                              [:animateable animation-state]
                              [:sprite sprite-state]
                              [:collidable collision-state]
                              [:damage damage-state]
                              :collision-debuggable
                              :ai])))
