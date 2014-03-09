(ns chocolatier.entities.monster
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Entity
                                                   Renderable
                                                   Moveable
                                                   Collidable]]
            [chocolatier.engine.primitives :refer [circle]]
            [chocolatier.engine.state :as s]))


(defrecord Monster [id sprite
                   ;; Where they are on the screen
                   screen-x screen-y
                   ;; Where they are on the world map
                   map-x map-y
                   ;; Which direction they are going :{n/s}{e/w}
                   direction
                   ;; How far x and y to move based on how fast they
                   ;; are moving
                   offset-x offset-y]

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
          (assoc this :sprite sprite))
        this)))

  Moveable
  (move [this state]
    (let [{:keys [screen-x screen-y]} this
          player (first (filter #(= (:id %) :player)
                                @(:entities state)))
          {:keys [offset-x offset-y]} player]
      ;; Apply the offset and reset offset to 0
      (assoc this
        :screen-x (+ screen-x offset-x)
        :screen-y (+ screen-y offset-y)
        :offset-x 0
        :offset-y 0)))

  Collidable
  (check-collision [this state time]
    this))

(defn create-monster!
  "Create a new entity and add to the list of global entities"
  [stage pos-x pos-y map-x map-y]
  (info "Creating monster" stage pos-x pos-y map-x map-y)
  (let [texture (js/PIXI.Texture.fromImage "static/images/monster.png")
        sprite (js/PIXI.Sprite. texture)
        monster (new Monster :monster sprite pos-x pos-y 0 0 :s 0 0)]
    (set! (.-position.x sprite) pos-x)
    (set! (.-position.y sprite) pos-y)
    (.addChild stage (:sprite monster))
    (swap! s/entities conj monster)))
