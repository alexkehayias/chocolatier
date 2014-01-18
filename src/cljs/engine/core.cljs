(ns chocolatier.engine.core
  (:use [chocolatier.utils.logging :only [debug info warn error]]
        [chocolatier.engine.systems.core :only [init-systems!]])
  (:require [dommy.core :as dom]
            ;; Imports the entity records
            ;; TODO can I explicitely import just the entity? or must
            ;; I always mark it with :as?
            [chocolatier.engine.entities :as e]
            [chocolatier.engine.state :as s]
            )
  (:use-macros [dommy.macros :only [node sel sel1]]))


;; TODO a function that takes a state hashmap and starts the game from it

(defn create-entity!
  "Create a new entity and add to the list of global entities"
  [stage img pos-x pos-y anc-x anc-y]
  (info "Creating entity" stage img pos-x pos-y anc-x anc-y)
  (let [texture (js/PIXI.Texture.fromImage img)
        sprite (js/PIXI.Sprite. texture)
        bunny (new e/Bunny (keyword (gensym)) sprite 0 0)]
    (set! (.-position.x (:sprite bunny)) pos-x)
    (set! (.-position.y (:sprite bunny)) pos-y)
    (set! (.-anchor.x (:sprite bunny)) anc-x)
    (set! (.-anchor.y (:sprite bunny)) anc-y)
    (swap! s/entities conj bunny)))

(defn iter-systems
  "Call each system registered in s/system in order with the 
   global state and time since last iteration"
  [state time]
  (doseq [[k f] (seq @(:systems state))]
    ;; (debug "iter-systems running" k)
    (f state time)))

(defn game-loop
  "Calculate changes based on the time since last change and 
   call all systems with the current state"
  [renderer stage]
  (when-not (:paused @s/game)
    ;; TODO Calculate the changes since the last game tick and pass
    ;; it to systems call
    ;; TODO use requestAnimation
    (iter-systems s/state nil)))

(defn set-interval
  "Creates an interval loop of n calls to function f per second.
   Returns a function to cancel the interval."
  [f n]
  (.setInterval js/window f (/ 1000 n))
  #(.clearInterval f (/ 1000 n)))

(defn init-game
  "Renders the game every n seconds.
   Hashmap of game properties."
  []
  (let [;; TODO reset the game height on screen resize
        width (aget js/window "innerWidth")
        height (aget js/window "innerHeight")
        frame-rate 60
        stage (js/PIXI.Stage. 0x66ff99)
        renderer (js/PIXI.CanvasRenderer. width height)
        _ (init-systems!)
        ;; TODO replace this with a function that calls each system
        ;; with the game state
        main-loop (set-interval #(game-loop renderer stage) 60)]
    (dom/append! (sel1 :body) (.-view renderer))
    ;; TODO return a function that also cleans up the canvas 
    {:renderer renderer
     :stage stage
     :end main-loop
     :paused false}))


;; Start, stop, reset the game. Game is stored in an atom and
;; referenced directly in these functions
(defn start-game! []
  (debug "Starting game")
  (reset! s/game (init-game)))

(defn stop-game!
  "Stop the game loop and remove the canvas"
  []
  (when-not (empty? @s/game)
    (debug "Removing the canvas")
    (dom/remove! (sel1 :canvas))
    (debug "Ending the game loop")
    (:end @s/game)))

(defn pause-game! []
  (swap! s/game assoc :paused true))

(defn unpause-game! []
  (swap! s/game assoc :paused false))

(defn reset-game! []
  (debug "Resetting game")
  (stop-game!)
  (s/reset-state!)
  (start-game!)
  (create-entity! (:stage @s/game)
                  "static/images/bunny.png"
                  500 500 0.05 0.05))
