(ns chocolatier.entities.enemy
  (:require [chocolatier.utils.logging :refer [debug info warn error]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]
            [chocolatier.engine.components.animateable :refer [mk-animateable-state]]
            [chocolatier.engine.components.collidable :refer [mk-collidable-state]]
            [chocolatier.engine.components.moveable :refer [mk-moveable-state]]))


(defn create-enemy!
  "Create a enemy by initializing some component state and adding the
   entity to state. Returns a function that takes a state hashmap."
  [state stage uid hit-radius]
  (let [texture (js/PIXI.Texture.fromImage "img/bunny.png")
        sprite (js/PIXI.Sprite. texture)
        [h w] (map #(aget sprite %) ["height" "width"])
        pos-x (* 1000 (js/Math.random))
        pos-y (* 1000 (js/Math.random))
        animation-state (mk-animateable-state stage
                                              "img/bunny.png"
                                              pos-x pos-y
                                              :standing
                                              [:standing 26 37 26 37 0 0 1])
        move-state (mk-moveable-state pos-x pos-y)
        collision-state (mk-collidable-state 26 37 hit-radius)]
    ;; Mutate the sprite and stage
    (.addChild stage sprite)
    (ces/mk-entity state
                   uid
                   :components [[:moveable move-state]
                                [:animateable animation-state]
                                [:collidable collision-state]
                                :collision-debuggable
                                :ai]
                   :subscriptions [[:move-change uid]
                                   [:collision uid]
                                   [:move uid]])))
