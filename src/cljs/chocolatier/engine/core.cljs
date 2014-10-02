(ns chocolatier.engine.core
  (:use [chocolatier.utils.logging :only [debug error]])
  (:require [dommy.core :as dom]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.input :refer [input-system]]
            [chocolatier.engine.systems.render :refer [render-system]]
            [chocolatier.engine.components.renderable :refer [update-sprite]])
  (:use-macros [dommy.macros :only [node sel sel1]]))


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
  [state scene-id]
  (let [systems (ces/get-system-fns state (-> state :scenes scene-id))
        updated-state (ces/iter-fns state systems)]
    (request-animation #(game-loop updated-state scene-id))))

;; TODO this should be used as a fallback if requestAnimationFrame is
;; not available in this browser
(defn set-interval
  "Creates an interval loop of n calls to function f per second.
   Returns a function to cancel the interval."
  [f n]
  (.setInterval js/window f (/ 1000 n))
  #(.clearInterval f (/ 1000 n)))

(defn start-game
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
        rendering-engine {:renderer renderer :stage stage}
        init-state (->  {:game {:rendering-engine rendering-engine}}
                        (ces/mk-scene :default [:input
                                                :render])
                        ;; Updates the user input from keyboard
                        (ces/mk-system :input input-system)
                        ;; TODO system for reacting to user input
                        ;; Render system for drawing sprites
                        (ces/mk-system :render render-system :renderable)
                        (ces/mk-component :renderable [update-sprite]))
        ;; PIXI requires a js array not a persistent vector
        assets (array "/static/images/bunny.png"
                      "/static/images/monster.png"
                      "/static/images/tile.png")
        asset-loader (new js/PIXI.AssetLoader assets)]
    (debug "Initial game state:" init-state)
    (debug "Loading assets")
    ;; Async load all the assets and build start the game on complete
    (aset asset-loader "onComplete"
          #(do
             (debug "Assets loaded")
             ;; Append the canvas to the dom
             (dom/append! (sel1 :body) (.-view renderer))
             ;; Start the game loop
             (game-loop init-state :default)))
    (.load asset-loader)))
