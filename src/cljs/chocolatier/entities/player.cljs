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
                   ;;
                   hit-radius]
  Entity
  (tick [this] this)

  Renderable
  (render [this state]
    (assoc this :offset-x 0 :offset-y 0))

  Collidable
  (check-collision [this state time]
    ;; Compare the screen x and y + offset x y and determine if there
    ;; is going to be a collision based on radius of each entity that
    ;; is collidable in range
    (let [entities @(:entities state)
          ;; Filter for entities that are not the player
          other-entities (filter #(not= this %) entities)
          ;; Check collision between this and all entities
          results (for [e other-entities]
                    (entity-collision? this e))]
      
      ;; FIX If we are colliding we must still be able to move away
      (if (some true? results)
        (do (swap! (:global state) assoc :offset-x 0 :offset-y 0)
            this)
        ;; Do nothing
        this)))

  UserInput
  ;; This should set the intended direction and movement NOT
  ;; commit it to the screen. Commits of movement need to happen in
  ;; the movement system
  (react-to-user-input [this state]
    (let [input @(:input state)
          move-rate 5
          move #(condp = %2
                  :W (assoc %1 :offset-y (* 1 move-rate) :direction :n)
                  :S (assoc %1 :offset-y (* -1 move-rate) :direction :s)
                  :D (assoc %1 :offset-x (* -1 move-rate) :direction :e)
                  :A (assoc %1 :offset-x (* 1 move-rate) :direction :w)
                  
                  :& (assoc %1 :offset-y (* 1 move-rate) :direction :n)
                  ;; Use keyword here since paranths are reserved
                  (keyword "(") (assoc %1 :offset-y (* -1 move-rate) :direction :s)
                  :' (assoc %1 :offset-x (* -1 move-rate) :direction :e)
                  :% (assoc %1 :offset-x (* 1 move-rate) :direction :w)                  
                  ;; Otherwise set the offset to 0 to denote the
                  ;; player is standing still
                  (assoc %1 :offset-x 0 :offset-y 0))]
      ;; Apply all the changes to the record in a recursive loop this
      ;; allows for handling key combinations
      (loop [out this
             i (seq input)]
        (let [[k v] (first i)
              remaining (rest i)
              updated (if (= v "on") (move out k) out)]
          (if (empty? remaining)
            (do
              (swap! (:global state) assoc
                     :offset-x (:offset-x updated)
                     :offset-y (:offset-y updated))
              updated) 
            (recur updated remaining)))))))

(defn create-player!
  "Create a new entity and add to the list of global entities"
  [stage pos-x pos-y map-x map-y hit-radius]
  (info "Creating player" pos-x pos-y map-x map-y hit-radius)
  (let [texture (js/PIXI.Texture.fromImage "static/images/bunny.png")
        sprite (js/PIXI.Sprite. texture)
        player (new Player :player sprite pos-x pos-y 0 0 :south 0 0 hit-radius)]
    (set! (.-position.x sprite) pos-x)
    (set! (.-position.y sprite) pos-y)
    (.addChild stage (:sprite player))
    (swap! s/entities conj player)))
