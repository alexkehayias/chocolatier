(ns chocolatier.core
  (:require [dommy.core :as dom]
            [chocolatier.utils.logging :refer [debug error]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.game :refer [init-state]]
            [chocolatier.engine.systems.tiles :refer [load-tilemap]])
  (:use-macros [dommy.macros :only [node sel sel1]]
               [chocolatier.macros :only [forloop local >> <<]]))

;; Controls game loop and allows dynamic changes to state even after
;; it is in the game loop
(def *running (atom true))
(def *state (atom nil))

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
  "Simple game loop using requestAnimation to optimize frame rate.
   If the atom running is false, returns the game state.

   Args:
   - state: an atom which is dereferenced at the start of the frame so
     if any changes are mode they can never happen in the middle of a frame
     being recalculated
   - scene-id: ignored"
  [game-state scene-id]
  (let [state (local game-state)
        systems (ces/get-system-fns game-state (-> game-state :scenes scene-id))]
    (forloop [[i 0] (< i (count systems)) (inc i)]
             (>> state ((systems i) (<< state))))
    ;; Copy the state into an atom so we can inspect while running
    (reset! *state (<< state))
    (if @*running
      (request-animation #(game-loop (<< state) scene-id))
      (debug "Game stopped"))))

;; TODO this should be used as a fallback if requestAnimationFrame is
;; not available in this browser
(defn set-interval
  "Creates an interval loop of n calls to function f per second.
   Returns a function to cancel the interval."
  [f n]
  (.setInterval js/window f (/ 1000 n))
  #(.clearInterval f (/ 1000 n)))

(defn -start-game!
  "Starts the game loop. This should be called only once all assets are loaded"
  [tilemap]
  (let [;; TODO reset the game height on screen resize
        width 800 ;; (aget js/window "innerWidth")
        height 600 ;; (aget js/window "innerHeight")
        frame-rate 60
        stage (new js/PIXI.Stage)
        renderer (new js/PIXI.CanvasRenderer width height nil true)
        init-timestamp (timestamp)
        init-duration 0
        step (/ 1 frame-rate)
        state (init-state renderer stage width height tilemap)]
    ;; Append the canvas to the dom
    (dom/append! (sel1 :body) (.-view renderer))
    ;; Start the game loop
    (game-loop state :default)))

(defn start-game!
  "Load all assets and call the tilemap loader. This is some async wankery to
   start the game."
  []
  (reset! *running true)
  (let [;; PIXI requires a js array not a persistent vector
        assets (array "/static/images/bunny.png"
                      "/static/images/monster.png"
                      "/static/images/tile.png"
                      "/static/images/snowtiles_1.gif"
                      "static/images/test_spritesheet.png")
        ;; Async load all the assets and start the game on complete
        asset-loader (new js/PIXI.AssetLoader assets)]
    (aset asset-loader "onComplete"
          ;; Async load the tilemap, on callback start the game
          #(load-tilemap "/static/tilemaps/snow_town_tile_map_v1.json"
                         -start-game!))
    (.load asset-loader)))

(defn cleanup! []
  (try (dom/remove! (sel1 :canvas))
       (catch js/Object e (error (str e)))))

(defn stop-game! []
  (reset! *running false) nil)

(defn restart-game! []
  (debug "Restarting...")
  (stop-game!)
  (cleanup!)
  ;; Give it a second to end the game
  (js/setTimeout start-game! 1000)
  nil)
