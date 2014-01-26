(ns chocolatier.entities.player
  ;; NOTE to use protocols from another ns, import them explicitely by
  ;; name and not their methods
  ;; Use them in this ns by refering to the name
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Entity
                                                   Renderable
                                                   UserInput]]))


(defrecord Player [id sprite x y]
  Entity
  (tick [this] this)
  
  Renderable
  (render [this stage]
    (let [sprite (:sprite this)]
      ;; TODO check the current vs the new and only set if it's new
      (set! (.-position.x sprite) (:x this))
      (set! (.-position.y sprite) (:y this))
      (assoc this :sprite sprite)))

  UserInput
  (react-to-user-input [this state time]
    ;; TODO update the player's velocity, direction
    ;; Maybe move this to collision detection system
    ;; Calculate given their velocity if they will be colliding with
    ;; something
    ;;(assoc this :x (+ 1 (:x this)))
    this
    ))
