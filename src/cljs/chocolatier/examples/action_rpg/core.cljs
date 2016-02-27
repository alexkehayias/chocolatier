(ns ^:figwheel-always chocolatier.examples.action-rpg.core
    (:require [dommy.core :as dom :refer-macros [sel sel1]]
              [devcards.core :as dc :refer-macros [defcard deftest dom-node]]
              [praline.core :refer [mount-inspector
                                    inspector-state
                                    inspector-app-state]]
              [chocolatier.utils.devcards :refer [str->markdown-code-block]]
              [chocolatier.utils.logging :refer [debug warn error]]
              [chocolatier.examples.action-rpg.game :refer [init-state]]
              [chocolatier.engine.systems.tiles :refer [load-tilemap]]
              [chocolatier.engine.systems.audio :refer [load-samples]]
              [chocolatier.engine.core :refer [game-loop]]))


(def *state* (atom nil))
(def *running* (atom true))

(defn wrap-state-inspector
  [f inspector-state inspector-app-state]
  (fn [state]
    (swap! inspector-state f)))

;; Some helpful middleware for the game loop
(defn wrap-fps-stats
  "Time how long it takes each frame to be calculated."
  [f stats-obj]
  (fn [state]
    (.begin stats-obj)
    (let [next-state (f state)]
      (.end stats-obj)
      next-state)))

(defn wrap-killswitch
  "Early exit the game by setting the running-atom to false.
   Needed for cleanup so we don't duplicate game loops every reload."
  [f running-atom]
  (fn [state]
    (if @running-atom
      (f state)
      (println "Terminating game loop"))))

(defn wrap-copy-state-to-atom
  "Copy the latest game state to the copy-atom so it can be inspected outside
   the game loop."
  [f copy-atom]
  (fn [state]
    (let [next-state (f state)]
      (reset! copy-atom next-state))))

(defn -start-game!
  "Starts the game loop. This should be called only once all assets are loaded."
  [node tilemap samples-library]
  ;; Make sure the running flag is true
  (reset! *running* true)
  (let [width (:width (dom/bounding-client-rect node))
        height 400
        stage (new js/PIXI.Container)
        options (clj->js {"transparent" true})
        renderer (new js/PIXI.CanvasRenderer width height options)
        stats-obj (new js/Stats)
        state (init-state renderer stage width height tilemap samples-library)
        inspector-state (inspector-state (update-in state [:state] dissoc :tiles))
        inspector-app-state (inspector-app-state)]

    ;; Append the canvas to the dom
    (dom/append! node (.-view renderer))

    ;; Position the stats module and append to dom
    (set! (.. stats-obj -domElement -style -position) "absolute")
    (set! (.. stats-obj -domElement -style -top) "0px")
    (set! (.. stats-obj -domElement -style -right) "0px")
    (dom/append! node (.-domElement stats-obj))

    ;; Setup the inspector
    (mount-inspector inspector-state inspector-app-state)

    (game-loop state
               (fn [handler]
                 (-> handler
                     (wrap-state-inspector inspector-state inspector-app-state)
                     (wrap-killswitch *running*)
                     ;; (wrap-copy-state-to-atom *state*)
                     (wrap-fps-stats stats-obj))))))

(defn start-game!
  "Load all assets and call the tilemap loader. This is some async wankery to
   start the game."
  [node]
  ;; TODO GET RID OF THESE MOTHERFUCKING CALLBACKS FOR LOADING ASSETS
  (let [;; Once the assets are loaded, load the tilemap
        audio-callback #(load-samples "/audio/samples" [:drip]
                                      (partial -start-game! node %))
        tiles-callback #(load-tilemap "/tilemaps/snow_town_tile_map_v1.json"
                                      audio-callback)]
    ;; Async load all the assets and start the game on complete

    ;; This will throw if there is already an entry for it so catch
    ;; any exceptions and then call the callback
    ;; TODO don't need these for sprites since they are loaded
    ;; dynamically if there is a cache miss
    (try (doto js/PIXI.loader
           (.add "bunny" "/img/bunny.png")
           (.add "tiles" "/img/snowtiles_1.gif")
           (.add "spritesheet" "/img/test_spritesheet.png")
           (.add "fireball" "/img/fireball.png"))
         (catch js/Object e (do (warn (str e))
                                (tiles-callback))))

    ;; Force the loading of assets and call the next callback
    (doto js/PIXI.loader
      (.once "complete" tiles-callback)
      (.load))))

(defn cleanup!
  "Clean up any elements that were generated"
  [node]
  (dom/clear! node))

(defn stop-game! []
  (reset! *running* false) nil)

(defn restart-game! [node]
  (debug "Restarting...")
  (stop-game!)
  (cleanup! node)
  ;; Give it a second to end the game
  (js/setTimeout #(start-game! node) 1000)
  nil)

(defcard "
# Action RPG Example

The example below utilizes the following systems and components:

- Collision detection (using rbush for spatial indexing)
- Keyboard input to control the player (WASD and slash to attack)
- Attack/damage/hitpoints/death
- Tile background via Tiled
- Sprite animation
- Movement
- Enemy AI (chases he player)
- Text labels that move with the entity
")

(defcard "
## Game
### Instructions
Use WASD to control the player and the forward slash key to attack.
"
  (dom-node
   (fn [_ node] (restart-game! node))))

(defcard "## Spec"
  (str->markdown-code-block
   (with-out-str (clojure.repl/source init-state))))

(defn on-js-reload
  "When figwheel reloads, this function gets called."
  [& args]
  ;; Only restart if there is a #main element
  (when-let [node (sel1 :#main)]
    (restart-game! node)))

;; Start the game on page load
(set! (.-onload js/window) on-js-reload)