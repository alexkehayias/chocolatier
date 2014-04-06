(ns chocolatier.entities.monster
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Entity
                                                   Renderable
                                                   Moveable
                                                   Collidable]]
            [chocolatier.engine.systems.collision :refer [entity-collision?]]
            [chocolatier.engine.state :as s]))


(defrecord Monster [id sprite screen-x screen-y map-x map-y
                    direction offset-x offset-y hit-radius]

  Entity
  (tick [this] this)
  
  Renderable
  ;; Apply the offsets and update the sprite
  (render [this state]
    (let [{:keys [sprite screen-x screen-y offset-x offset-y]} this
          [sprite-x sprite-y] (map #(aget sprite "position" %) ["x" "y"])]
      ;; Only update the sprite if the new screen position does not
      ;; match the sprite's position
      (if (or (not= sprite-x (+ screen-x offset-x))
              (not= sprite-y (+ screen-y offset-y)))
        ;; Update the sprite position and the screen position
        (do
          (set! (.-position.x sprite) (+ screen-x offset-x))
          (set! (.-position.y sprite) (+ screen-y offset-y))
          (assoc this
            :sprite sprite
            :screen-x (+ screen-x offset-x)
            :screen-y (+ screen-y offset-y)
            :offset-x 0 :offset-y 0))
        (assoc this :offset-x 0 :offset-y 0))))

  Moveable
  ;; Mirror the players movements
  (move [this state]
    (let [{:keys [offset-x offset-y]} @(:global state)]
      (assoc this :offset-x offset-x :offset-y offset-y)))

  Collidable
  (check-collision [this state time]
    (let [entities @(:entities state)          
          other-entities (filter #(not= this %) entities)
          results (for [e other-entities] (entity-collision? this e))]
      (if (some true? results)
        (assoc this :offset-x 0 :offset-y 0)
        this))))

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
