(ns chocolatier.entities.enemy
  (:require [chocolatier.utils.logging :refer [debug info warn error]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))

(def texture (js/PIXI.Texture.fromImage "static/images/bunny.png"))

(defn create-enemy!
  "Create a enemy by initializing some component state and adding the 
   entity to state. Returns a function that takes a state hashmap."
  [state stage uid hit-radius]
  (let [sprite (js/PIXI.Sprite. texture)
        [h w] (map #(aget sprite %) ["height" "width"])
        init-render-state {:sprite sprite
                           :texture texture
                           :height h :width w
                           :pos-x (* 1000 (js/Math.random))
                           :pos-y (* 1000 (js/Math.random))
                           :hit-radius hit-radius}]
    (info "Creating enemy" pos-x pos-y map-x map-y hit-radius)
    ;; Mutate the sprite and stage
    (set! (.-position.x sprite) pos-x)
    (set! (.-position.y sprite) pos-y)
    (.addChild stage sprite)
    (-> state
        (assoc-in [:state :renderable uid] init-render-state)
        (ces/mk-entity uid [:renderable
                            :collidable
                            :collision-debuggable
                            :ai
                            :moveable])
        (ev/subscribe uid :move-change uid)
        (ev/subscribe uid :collision uid)
        (ev/subscribe uid :move uid))))
