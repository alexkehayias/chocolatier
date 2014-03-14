(ns chocolatier.entities.player
  ;; NOTE to use protocols from another ns, import them explicitely by
  ;; name and not their methods
  ;; Use them in this ns by refering to the name
  (:require [chocolatier.utils.logging :refer [debug info warn error]]
            [chocolatier.engine.components :refer [Entity
                                                   Renderable
                                                   UserInput
                                                   Moveable
                                                   Collidable]]
            [chocolatier.engine.systems.collision :refer [collision?]]
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
    (let [sprite (:sprite this)
          {:keys [screen-x screen-y]} this
          [sprite-x sprite-y] (map #(aget sprite "position" %) ["x" "y"])]
      ;; TODO only move the player on the screen if we are at the edge of a map
      ;; (if (or (not= sprite-x screen-x) (not= sprite-y screen-y))
      ;;   (do
      ;;     (set! (.-position.x sprite) screen-x)
      ;;     (set! (.-position.y sprite) screen-y)
      ;;     (assoc this :sprite sprite))
      ;;   this)
      ;; Clear out remaining offset
      (assoc this :offset-x 0 :offset-y 0)))

  Collidable
  (check-collision [this state time]
    ;; Compare the screen x and y + offset x y and determine if there
    ;; is going to be a collision based on radius of each entity that
    ;; is collidable in range
    (let [entities @(:entities state)          
          ;; Filter for entities that are not the player
          other-entities (filter #(not= this %) entities)          
          {:keys [screen-x screen-y offset-x offset-y hit-radius]} this
          ;; Apply the offsets as if they were happening
          [x1 y1] (map + [screen-x screen-y] [offset-x offset-y])
          r1 hit-radius
          results (for [e other-entities]
                    (let [{:keys [screen-x screen-y offset-x offset-y hit-radius]} e
                          [x2 y2] (map + [screen-x screen-y] [offset-x offset-y])
                          r2 hit-radius]
                      (when (satisfies? Collidable e)
                        (collision? x1 y1 r1 x2 y2 r2))))]

      ;; FIX if they are colliding we must be able to move away from
      ;; the collision
      (if (some true? results)
        ;; Stop the player's movement
        (do
          (debug "Collision detected!")
          (assoc this :offset-x 0 :offset-y 0)) 
        ;; Do nothing
        this)))

  UserInput
  ;; This should set the intended direction and movement NOT
  ;; commit it to the screen. Commits of movement need to happen in
  ;; the movement system
  (react-to-user-input [this state]
    (let [input @(:input state)
          move-rate 5.0
          move #(condp = %2
                  :W (assoc %1 :offset-y (* 1 move-rate))
                  :A (assoc %1 :offset-x (* 1 move-rate))
                  :S (assoc %1 :offset-y (* -1 move-rate))
                  :D (assoc %1 :offset-x (* -1 move-rate))
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
            updated
            (recur updated remaining)))))))

(defn create-player!
  "Create a new entity and add to the list of global entities"
  [stage pos-x pos-y map-x map-y hit-radius]
  (info "Creating player" pos-x pos-y map-x map-y hit-radius)
  (let [texture (js/PIXI.Texture.fromImage "static/images/bunny.png")
        sprite (js/PIXI.Sprite. texture)
        player (new Player :player sprite pos-x pos-y 0 0 :s 0 0 hit-radius)]
    (set! (.-position.x sprite) pos-x)
    (set! (.-position.y sprite) pos-y)
    (.addChild stage (:sprite player))
    (swap! s/entities conj player)))
