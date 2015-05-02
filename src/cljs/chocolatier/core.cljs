(ns ^:figwheel-always chocolatier.core
  (:require [dommy.core :as dom :refer-macros [sel sel1]]
            [chocolatier.utils.logging :refer [debug error]]
            [chocolatier.game :refer [init-state]]
            [chocolatier.engine.systems.tiles :refer [load-tilemap]]
            [chocolatier.engine.core :refer [game-loop *running* *state*]]))


(defn -start-game!
  "Starts the game loop. This should be called only once all assets are loaded"
  [tilemap]
  (let [;; TODO reset the game height on screen resize
        width 800 ;; (aget js/window "innerWidth")
        height 600 ;; (aget js/window "innerHeight")
        stage (new js/PIXI.Stage)
        renderer (new js/PIXI.CanvasRenderer width height nil true)
        state (init-state renderer stage width height tilemap)]
    ;; Append the canvas to the dom
    (dom/append! (sel1 :body) (.-view renderer))
    ;; Start the game loop
    (game-loop state)))

(defn start-game!
  "Load all assets and call the tilemap loader. This is some async wankery to
   start the game."
  []
  (reset! *running* true)
  (let [;; PIXI requires a js array not a persistent vector
        assets (array "/img/bunny.png"
                      "/img/monster.png"
                      "/img/tile.png"
                      "/img/snowtiles_1.gif"
                      "/img/test_spritesheet.png")
        ;; Async load all the assets and start the game on complete
        asset-loader (new js/PIXI.AssetLoader assets)]
    (aset asset-loader "onComplete"
          ;; Async load the tilemap, on callback start the game
          #(load-tilemap "/tilemaps/snow_town_tile_map_v1.json"
                         -start-game!))
    (.load asset-loader)))

(defn cleanup! []
  (try (dom/remove! (sel1 :canvas))
       (catch js/Object e (error (str e)))))

(defn stop-game! []
  (reset! *running* false) nil)

(defn restart-game! []
  (debug "Restarting...")
  (stop-game!)
  (cleanup!)
  ;; Give it a second to end the game
  (js/setTimeout start-game! 1000)
  nil)
