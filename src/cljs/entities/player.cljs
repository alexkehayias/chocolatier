(ns chocolatier.entities.player
  ;; NOTE to use protocols from another ns, import them explicitely by
  ;; name and not their methods
  ;; Use them in this ns by refering to the name
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Entity
                                                   Renderable
                                                   UserInput
                                                   Moveable
                                                   Collidable]]
            [chocolatier.engine.state :as s]))


(defrecord Player [id sprite
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
      ;; TODO only move the player on the screen if we are at the edge of a map
      ;; (if (or (not= sprite-x screen-x) (not= sprite-y screen-y))
      ;;   (do
      ;;     (set! (.-position.x sprite) screen-x)
      ;;     (set! (.-position.y sprite) screen-y)
      ;;     (assoc this :sprite sprite))
      ;;   this)
      ;; Clear out remaining offset
      (assoc this :offset-x 0 :offset-y 0)))

  Moveable
  ;; Apply the offset to the screen x and y
  (move [this state]
    (let [{:keys [screen-x screen-y offset-x offset-y]} this]
      this))

  Collidable
  (check-collision [this state time]
    this)

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
  [stage pos-x pos-y map-x map-y]
  (info "Creating player" stage "static/images/bunny.png" pos-x pos-y map-x map-y)
  (let [texture (js/PIXI.Texture.fromImage "static/images/bunny.png")
        sprite (js/PIXI.Sprite. texture)
        player (new Player :player sprite pos-x pos-y 0 0 :s 0 0)]
    (set! (.-position.x sprite) pos-x)
    (set! (.-position.y sprite) pos-y)
    (.addChild stage (:sprite player))
    (swap! s/entities conj player)))
