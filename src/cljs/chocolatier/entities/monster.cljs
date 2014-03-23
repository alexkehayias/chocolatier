(ns chocolatier.entities.monster
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Entity
                                                   Renderable
                                                   Moveable
                                                   Collidable]]
            [chocolatier.engine.state :as s]))


(defrecord Monster [id sprite screen-x screen-y map-x map-y
                    direction offset-x offset-y hit-radius]

  Entity
  (tick [this] this)
  
  Renderable
  (render [this state]
    (let [sprite (:sprite this)
          {:keys [screen-x screen-y]} this
          [sprite-x sprite-y] (map #(aget sprite "position" %) ["x" "y"])]
      (if (or (not= sprite-x screen-x) (not= sprite-y screen-y))
        (do
          (set! (.-position.x sprite) screen-x)
          (set! (.-position.y sprite) screen-y)
          (assoc this :sprite sprite
                      :offset-x 0
                      :offset-y 0))
        (assoc this :offset-x 0 :offset-y 0))))

  Moveable
  (move [this state]
    (let [{:keys [screen-x screen-y]} this
          player (first (filter #(= (:id %) :player)
                                @(:entities state)))
          {:keys [offset-x offset-y]} player]
      ;; Apply the offset and reset offset to 0
      (assoc this
        :screen-x (+ screen-x offset-x)
        :screen-y (+ screen-y offset-y))))

  Collidable
  (check-collision [this state time] this))

(defn create-monster!
  "Create a new entity and add to the list of global entities"
  [stage pos-x pos-y map-x map-y hit-radius]
  (info "Creating monster" pos-x pos-y map-x map-y)
  (let [texture (js/PIXI.Texture.fromImage "static/images/monster.png")
        sprite (js/PIXI.Sprite. texture)
        monster (new Monster :monster sprite pos-x pos-y 0 0 :s 0 0 hit-radius)]
    (set! (.-position.x sprite) pos-x)
    (set! (.-position.y sprite) pos-y)
    (.addChild stage (:sprite monster))
    (swap! s/entities conj monster)))
