(ns chocolatier.entities.player
  (:require [chocolatier.utils.logging :refer [debug info warn error]]
            [chocolatier.engine.ces :as ces]))


(defn create-player!
  "Create a player by initializing some component state and adding the 
   entity to state. Returns a function that takes a state hashmap."
  [stage uid pos-x pos-y map-x map-y hit-radius]
  (fn [state]
    (let [texture (js/PIXI.Texture.fromImage "static/images/bunny.png")
          sprite (js/PIXI.Sprite. texture)
          [h w] (map #(aget sprite %) ["height" "width"])
          init-render-state {:sprite sprite
                             :texture texture
                             :height h :width w
                             :map-x map-x :map-y map-y
                             :pos-x pos-x :pos-y pos-y
                             :offset-x 0 :offset-y 0
                             :hit-radius hit-radius}]
      (info "Creating player" pos-x pos-y map-x map-y hit-radius)
      ;; Mutate the sprite and stage
      (set! (.-position.x sprite) pos-x)
      (set! (.-position.y sprite) pos-y)
      (.addChild stage sprite)
      (-> state
          (assoc-in [:state :renderable uid] init-render-state)
          (ces/mk-entity uid [:renderable :controllable :collidable :collision-debuggable])))))
