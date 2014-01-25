(ns chocolatier.engine.core
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [dommy.core :as dom]
            ;; Imports the entity records
            ;; TODO can I explicitely import just the entity? or must
            ;; I always mark it with :as?
            [chocolatier.entities.player :as p]
            [chocolatier.engine.state :as s]
            [chocolatier.engine.systems.core :refer [init-systems!]]
            [chocolatier.engine.input :refer [reset-input!]]
            [chocolatier.engine.systems.render :refer [render-system]])
  (:use-macros [dommy.macros :only [node sel sel1]]))


;; TODO a function that takes a state hashmap and starts the game from it

(defn create-entity!
  "Create a new entity and add to the list of global entities"
  [stage img pos-x pos-y anc-x anc-y]
  (info "Creating entity" stage img pos-x pos-y anc-x anc-y)
  (let [texture (js/PIXI.Texture.fromImage img)
        sprite (js/PIXI.Sprite. texture)
        player (new p/Player (keyword (gensym)) sprite 0 0)]
    (set! (.-position.x (:sprite player)) pos-x)
    (set! (.-position.y (:sprite player)) pos-y)
    (set! (.-anchor.x (:sprite player)) anc-x)
    (set! (.-anchor.y (:sprite player)) anc-y)
    (.addChild stage (:sprite player))
    (swap! s/entities conj player)))

(defn iter-systems
  "Call each system registered in s/system in order with the 
   global state and fixed time to compute."
  [state time]
  (doseq [[k f] (seq @(:systems state))]
    (f state time)))

(defn request-animation [f]
  (js/requestAnimationFrame f))

(defn timestamp
  "Get the current timestamp using performance.now. 
   Fall back to Date.getTime for older browsers"
  []
  (if (and (exists? (aget js/window "performance"))
           (exists? (aget js/window "performance" "now")))
    (js/window.performance.now)
    ((aget (new js/Date) "getTime"))))

(defn game-loop
  "Fixed timestep gameloop that calculates changes based on the 
   time duration since last run.

   Calls all systems with the current state n times where n
   is the number of steps in the duration.

   The render system is called separately."
  [last-timestamp duration step]
  (let [now (timestamp)
        delta (- now last-timestamp)
        ;; Minimum duration processed is 1 second        
        duration (+ duration (Math.min 1 (/ delta 1000)))]
    ;; Throw an error if we get into a bad state
    (when (< duration 0)
      (throw (js/Error. "Bad state, duration is < 0")))
    (when-not (:stop @s/game)
      (loop [dt duration]
        (if (< (- dt step) step)
          ;; Break the loop, render, and request the next frame
          (do
            (render-system s/state)
            (request-animation #(game-loop now dt step)))
          (if (:paused @s/game)
            (recur (- dt step))
            (do (iter-systems s/state step)
                (recur (- dt step)))))))))

(defn set-interval
  "Creates an interval loop of n calls to function f per second.
   Returns a function to cancel the interval."
  [f n]
  (.setInterval js/window f (/ 1000 n))
  #(.clearInterval f (/ 1000 n)))

;; Start, stop, reset the game. Game and all state is stored in
;; an atom and referenced directly in the game loop

(defn start-game!
  "Renders the game every n seconds.
   Hashmap of game properties."
  []
  (let [;; TODO reset the game height on screen resize
        width (aget js/window "innerWidth")
        height (aget js/window "innerHeight")
        frame-rate 60
        stage (js/PIXI.Stage. 0x66ff99)
        renderer (js/PIXI.CanvasRenderer. width height)
        init-timestamp (timestamp)
        init-duration 0
        step (/ 1 frame-rate)
        game-state {:renderer renderer
                    :stage stage
                    :stop false
                    :paused false}]
    (dom/append! (sel1 :body) (.-view renderer))
    (init-systems!)
    (reset-input!)
    (reset! s/game game-state)
    ;; Start the game loop
    (request-animation #(game-loop init-timestamp init-duration step))))

(defn stop-game!
  "Stop the game loop and remove the canvas"
  []
  (when-not (empty? @s/game)
    (debug "Ending the game loop")
    (swap! s/game assoc :stop true)
    (debug "Removing the canvas")
    (dom/remove! (sel1 :canvas))))

(defn pause-game! []
  (swap! s/game assoc :paused true))

(defn unpause-game! []
  (swap! s/game assoc :paused false))

(defn reset-game! []
  (debug "Resetting game")
  (stop-game!)
  (s/reset-state!)
  (start-game!)
  (let [x (/ (aget js/window "innerWidth") 2) 
        y (/ (aget js/window "innerHeight") 2)]
    (create-entity! (:stage @s/game)
                    "static/images/bunny.png"
                    x y 0 0)))
