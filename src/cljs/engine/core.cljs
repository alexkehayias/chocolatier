(ns chocolatier.engine.core
  (:use [chocolatier.utils.logging :only [debug error]])
  (:require [dommy.core :as dom]
            ;; Imports the entity records
            ;; TODO can I explicitely import just the entity? or must
            ;; I always mark it with :as?
            [chocolatier.entities.player :as p]
            [chocolatier.entities.monster :as m]
            [chocolatier.engine.state :as s]
            [chocolatier.engine.systems.core :refer [reset-systems!]]
            [chocolatier.engine.input :refer [reset-input!]]
            [chocolatier.engine.tiling :refer [load-test-tile-map!]]
            [chocolatier.engine.systems.render :refer [render-system]])
  (:use-macros [dommy.macros :only [node sel sel1]]))


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

   The render system is called separately so that animation stays smooth."
  [last-timestamp duration step]
  (let [now (timestamp)
        delta (- now last-timestamp)
        ;; Minimum duration processed is 1 second        
        duration (+ duration (Math.min 1 (/ delta 1000)))]
    
    ;; Throw an error if we get into a bad state
    (when (< duration 0)
      (throw (js/Error. "Bad state, duration is < 0")))

    ;; Check if the stop flag has been thrown otherwise iterate
    ;; through all systems then request the next animation
    (if (-> s/state :game deref :stop)
      (do (debug ":stop flag thrown, stopping game loop")
          (swap! (:game s/state) assoc :stopped true))
      ;; Calculate all changes for each step in the duration since the
      ;; last run through the game loop
      (loop [dt duration]
        (if (< (- dt step) step)
          ;; Break the loop, render, and request the next frame
          (do
            (render-system s/state)
            (request-animation #(game-loop now dt step)))
          ;; If the game is paused, keep the loop going but don't
          ;; calculate any changes
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
        _ (debug "Setting renderer to" width "x" height "frame-rate" frame-rate)
        stage (new js/PIXI.Stage)
        renderer (new js/PIXI.CanvasRenderer width height nil true)
        init-timestamp (timestamp)
        init-duration 0
        step (/ 1 frame-rate)
        game-state {:renderer renderer
                    :stage stage}
        ;; PIXI requires a js array not a persistent vector
        assets (array "/static/images/bunny.png"
                      "/static/images/monster.png"
                      "/static/images/tile.png")
        asset-loader (new js/PIXI.AssetLoader assets)]
    (debug "Loading assets")
    ;; Async load all the assets and build start the game on complete
    (aset asset-loader "onComplete"
          #(do
             (debug "Assets loaded")
             ;; Append the canvas to the dom
             (dom/append! (sel1 :body) (.-view renderer))
             
             ;; Initialize state
             (s/reset-state!)
             (reset-systems!)
             (reset-input!)
             (reset! s/game game-state)

             ;; Initial game tiles and player
             (load-test-tile-map! stage)
             (p/create-player! stage (/ width 2) (/ height 2) 0 0)
             (m/create-monster! stage (/ width 3) (/ height 3) 0 0)
             
             ;; Start the game loop
             (game-loop init-timestamp init-duration step)))
    (.load asset-loader)))

(defn stop-game!
  "Stop the game loop and remove the canvas"
  [callback]
  (if @s/game
    (do
      (debug "Ending the game loop")
      ;; Throws a state flag to stop the game-loop
      (swap! s/game assoc :stop true)
      ;; Watch for the game loop to confirm the stop
      (add-watch s/game :game-end
                 (fn [k s o n] (when (:stopped n) (callback)))))
    (callback)))

(defn cleanup! []
  (remove-watch s/game :game-end)
  (try (dom/remove! (sel1 :canvas))
       (catch js/Object e (error (str e)))))

(defn pause-game! []
  (swap! s/game assoc :paused true))

(defn unpause-game! []
  (swap! s/game assoc :paused false))

(defn reset-game! []
  (debug "Resetting game")
  (stop-game! #(do (cleanup!)
                   (start-game!)))
  nil)
