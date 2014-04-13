(ns chocolatier.entities.player
  ;; NOTE to use protocols from another ns, import them explicitely by
  ;; name and not their methods
  ;; Use them in this ns by refering to the name
  (:require [chocolatier.utils.logging :refer [debug info warn error]]
            [chocolatier.engine.components :refer [Entity
                                                   Renderable
                                                   Moveable
                                                   UserInput
                                                   Collidable]]
            [chocolatier.engine.systems.collision :refer [entity-collision?]]
            [chocolatier.engine.state :as s]))


(def move-rate 4)

(def keycode->direction
  {:W :n
   :S :s
   :D :e
   :A :w
   ;; Direction pad
   :& :n
   ;; Use keyword here since paranths are reserved
   (keyword "(") :s
   :' :e
   :% :w})

(def keycode->offset
  {:W [:offset-y (* 1 move-rate)]
   :S [:offset-y (* -1 move-rate)]
   :D [:offset-x (* -1 move-rate)]
   :A [:offset-x (* 1 move-rate)]
   ;; Direction pad   
   :& [:offset-y (* 1 move-rate)]
   ;; Use keyword here since parahs are reserved
   (keyword "(") [:offset-y (* -1 move-rate)]
   :' [:offset-x (* -1 move-rate)]
   :% [:offset-x (* 1 move-rate)]})

(defrecord Player [id
                   ;; A PIXI sprite object
                   sprite
                   ;; Where they are on the screen
                   screen-x screen-y
                   ;; Where they are on the world map
                   map-x map-y
                   ;; Which direction they are facing :{n/s}{e/w}
                   direction
                   ;; How far x and y to move based on how fast they
                   ;; are moving and direction
                   offset-x offset-y
                   hit-radius
                   ;; The height and width of the sprite
                   height width]
  Entity
  (tick [this] this)

  Renderable
  (render [this state] this)

  Collidable
  (check-collision [this state time]
    ;; Compare the screen x and y + offset x y and determine if there
    ;; is going to be a collision based on radius of each entity that
    ;; is collidable in range
    (let [entities (for [[k v] (seq @(:entities state))] v)
          ;; Filter for entities that are not the player
          other-entities (filter #(not= this %) entities)
          ;; Check collision between this and all entities
          adj-this (assoc this :offset-x 0 :offset-x 0)          
          results (for [e other-entities]
                    (entity-collision? adj-this e))]
      
      ;; FIX If we are colliding we must still be able to move away
      (if (some true? results)
        ;; TODO we should apply the reverse force of the current move
        ;; instead of doing a straight zero because what if the
        ;; moves before in the iter-systems were valid?
        (do (swap! (:global state) assoc :offset-x 0 :offset-y 0)
            (assoc this :offset-x 0 :offset-y 0))
        ;; Do nothing
        this)))

  UserInput
  (react-to-user-input [this state]
    ;; Recursively loop through all keycodes to get the global offset
    ;; allows for handling key combinations
    (loop [offsets {:offset-x 0 :offset-y 0}
           i (seq @(:input state))]
      (let [[k v] (first i)
            remaining (rest i)
            updated-offsets (if (= v "on")
                              (apply assoc offsets (k keycode->offset))
                              offsets)]
        (if (empty? remaining)
          ;; Update the global offsets
          (swap! (:global state) assoc
                 :offset-x (:offset-x updated-offsets) 
                 :offset-y (:offset-y updated-offsets)) 
          (recur updated-offsets remaining))))
    ;; TODO set the players direction
    this))

(defn create-player!
  "Create a new entity and add to the global entities hashmap"
  [stage pos-x pos-y map-x map-y hit-radius]
  (info "Creating player" pos-x pos-y map-x map-y hit-radius)
  (let [texture (js/PIXI.Texture.fromImage "static/images/bunny.png")
        sprite (js/PIXI.Sprite. texture)
        ;; Coerce the height and width from the sprite
        [h w] (map #(aget sprite %) ["height" "width"])
        player (new Player :player sprite pos-x pos-y 0 0 :s 0 0 hit-radius h w)]
    (set! (.-position.x sprite) pos-x)
    (set! (.-position.y sprite) pos-y)
    (.addChild stage (:sprite player))
    (swap! s/entities assoc :player player)))
