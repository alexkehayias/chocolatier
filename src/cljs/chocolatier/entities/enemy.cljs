(ns chocolatier.entities.enemy
  (:require [chocolatier.utils.logging :refer [debug info warn error]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]
            [chocolatier.engine.components.animateable :as a]))

(def texture (js/PIXI.Texture.fromImage "static/images/bunny.png"))

(defn create-enemy!
  "Create a enemy by initializing some component state and adding the 
   entity to state. Returns a function that takes a state hashmap."
  [state stage uid hit-radius]
  (let [sprite (js/PIXI.Sprite. texture)
        [h w] (map #(aget sprite %) ["height" "width"])
        pos-x (* 1000 (js/Math.random))
        pos-y (* 1000 (js/Math.random))
        init-moveable-state {:pos-x pos-x
                             :pos-y pos-y}
        init-animateable-state (a/mk-animation-state
                                stage
                                "static/images/bunny.png"
                                pos-x pos-y
                                :standing
                                [:standing 26 37 26 37 0 0 1])]
    ;; Mutate the sprite and stage
    (.addChild stage sprite)
    (-> state
        (assoc-in [:state :moveable uid] init-moveable-state)
        (assoc-in [:state :animateable uid] init-animateable-state)        
        (ces/mk-entity uid [:moveable
                            :animateable
                            :collidable
                            :collision-debuggable
                            :ai])
        (ev/subscribe uid :move-change uid)
        (ev/subscribe uid :collision uid)
        (ev/subscribe uid :move uid))))
