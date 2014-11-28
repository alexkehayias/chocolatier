(ns chocolatier.engine.core
  (:use [chocolatier.utils.logging :only [debug error]])
  (:require [dommy.core :as dom]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.input :refer [input-system]]
            [chocolatier.engine.systems.user-input :refer [user-input-system]]
            [chocolatier.engine.systems.render :refer [render-system]]
            [chocolatier.engine.systems.collision :refer [broad-collision-system
                                                          narrow-collision-system]]
            [chocolatier.engine.systems.tiles :refer [tile-system create-tiles!]]
            [chocolatier.engine.systems.events :refer [event-system
                                                       init-events-system]]
            [chocolatier.engine.systems.debug :refer [debug-collision-system]]
            [chocolatier.engine.systems.movement :refer [movement-system]]
            [chocolatier.engine.systems.ai :refer [ai-system]]
            [chocolatier.engine.components.renderable :refer [update-sprite]]
            [chocolatier.engine.components.controllable :refer [react-to-input
                                                                include-input-state]]
            [chocolatier.engine.components.collidable :refer [check-collisions]]
            [chocolatier.engine.components.debuggable :refer [draw-collision-zone
                                                              include-renderable-state-and-stage]]
            [chocolatier.engine.components.moveable :refer [move
                                                            include-renderable-state]]
            [chocolatier.engine.components.ai :refer [behavior
                                                      include-player-and-renderable-state]]
            [chocolatier.entities.player :refer [create-player!]]
            [chocolatier.entities.enemy :refer [create-enemy!]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

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
  [state scene-id]
  (let [systems (ces/get-system-fns @state (-> @state :scenes scene-id))]
    (swap! state #(ces/iter-fns % systems))
    (if @*running
      (request-animation #(game-loop state scene-id))
      (debug @state))))

;; TODO this should be used as a fallback if requestAnimationFrame is
;; not available in this browser
(defn set-interval
  "Creates an interval loop of n calls to function f per second.
   Returns a function to cancel the interval."
  [f n]
  (.setInterval js/window f (/ 1000 n))
  #(.clearInterval f (/ 1000 n)))

(defn start-game!
  "Renders the game every n seconds."
  []
  (reset! *running true)
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
        mk-player-1 (create-player! stage :player1 20 20 0 0 20)
        mk-tiles (create-tiles! stage)
        init-state (-> {:game {:rendering-engine rendering-engine}
                        :state {:events {:queue []}}}
                       ;; A collection of keys representing systems
                       ;; that will be called in sequential order
                       (ces/mk-scene :default [:input
                                               :user-input
                                               :ai
                                               :broad-collision
                                               :narrow-collision
                                               :collision-debug
                                               :movement
                                               :tiles
                                               :render])
                       ;; Global event system broadcaster
                       (init-events-system)
                       (ces/mk-system :events event-system)
                       ;; Updates the user input from keyboard,
                       ;; standalone system with no components
                       (ces/mk-system :input input-system)
                       ;; React to user input
                       (ces/mk-system :user-input user-input-system :controllable)
                       (ces/mk-component :controllable
                                         ;; Calls react-to-input
                                         ;; with additional argument
                                         ;; for the current input
                                         ;; state 
                                         [[react-to-input {:args-fn include-input-state}]])
                       ;; Draw tile map in background
                       (ces/mk-system :tiles tile-system)
                       ;; Initial tile map
                       (mk-tiles)
                       ;; Render system for drawing sprites
                       (ces/mk-system :render render-system :renderable)
                       (ces/mk-component :renderable [update-sprite])
                       ;; Collision detection system
                       (ces/mk-system :broad-collision (broad-collision-system (/ width 20)))
                       (ces/mk-system :narrow-collision narrow-collision-system)
                       (ces/mk-system :collision-debug debug-collision-system :collision-debuggable)
                       (ces/mk-component :collision-debuggable [[draw-collision-zone
                                                                 {:args-fn include-renderable-state-and-stage}]])
                       (ces/mk-system :movement movement-system :moveable)
                       (ces/mk-component :moveable [[move
                                                     {:args-fn include-renderable-state}]])
                       (ces/mk-system :ai ai-system :ai)
                       (ces/mk-component :ai [[behavior
                                               {:args-fn include-player-and-renderable-state}]])
                       ;; Player 1 entity
                       (mk-player-1)

                       ;; Other entities
                       (ces/iter-fns (for [i (range 50)]
                                       #(create-enemy! % stage (keyword (gensym)) 20))) )
        ;; PIXI requires a js array not a persistent vector
        assets (array "/static/images/bunny.png"
                      "/static/images/monster.png"
                      "/static/images/tile.png")
        asset-loader (new js/PIXI.AssetLoader assets)]
    (debug "Loading game state into atom")
    (reset! *state init-state)    
    ;; Async load all the assets and start the game on complete
    (aset asset-loader "onComplete"
          #(do (debug "Assets loaded")
               ;; Append the canvas to the dom    
               (dom/append! (sel1 :body) (.-view renderer))             
               ;; Start the game loop
               (game-loop *state :default)))
    ;; Call the asset-loader
    (.load asset-loader)))

(defn cleanup! []
  (try (dom/remove! (sel1 :canvas))
       (catch js/Object e (error (str e)))))

(defn stop-game! []
  (reset! *running false))

(defn restart-game! []
  (debug "Restarting...")
  (stop-game!)
  (cleanup!)
  ;; Give it a second to end the game
  (js/setTimeout start-game! 1000)
  nil)
