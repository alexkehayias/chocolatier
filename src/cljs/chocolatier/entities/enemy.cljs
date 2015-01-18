(ns chocolatier.entities.enemy
  (:require [chocolatier.utils.logging :refer [debug info warn error]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]
            [chocolatier.engine.components.animateable :refer [mk-animateable-state]]
            [chocolatier.engine.components.collidable :refer [mk-collidable-state]]
            [chocolatier.engine.components.moveable :refer [mk-moveable-state]]))

(def texture (js/PIXI.Texture.fromImage "static/images/bunny.png"))

(defn create-enemy!
  "Create a enemy by initializing some component state and adding the 
   entity to state. Returns a function that takes a state hashmap."
  [state stage uid hit-radius]
  (let [sprite (js/PIXI.Sprite. texture)
        [h w] (map #(aget sprite %) ["height" "width"])
        pos-x (* 1000 (js/Math.random))
        pos-y (* 1000 (js/Math.random))]
    ;; Mutate the sprite and stage
    (.addChild stage sprite)
    (-> state
        (mk-animateable-state stage
                              uid
                              "static/images/bunny.png"
                              pos-x pos-y
                              :standing
                              [:standing 26 37 26 37 0 0 1])
        (mk-moveable-state uid pos-x pos-y)
        (mk-collidable-state uid 26 37 hit-radius)
        (ces/mk-entity uid [:moveable
                            :animateable
                            :collidable
                            :collision-debuggable
                            :ai])
        (ev/subscribe uid :move-change uid)
        (ev/subscribe uid :collision uid)
        (ev/subscribe uid :move uid))))
