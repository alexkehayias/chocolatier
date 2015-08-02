(ns chocolatier.game
  (:require [chocolatier.engine.core :refer [mk-game-state]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.input :refer [input-system]]
            [chocolatier.engine.systems.user-input :refer [user-input-system]]
            [chocolatier.engine.systems.render :refer [render-system]]
            [chocolatier.engine.systems.animation :refer [animation-system]]
            [chocolatier.engine.systems.collision :refer [broad-collision-system
                                                          narrow-collision-system]]
            [chocolatier.engine.systems.tiles :refer [tile-system load-tilemap mk-tiles-from-tilemap!]]
            [chocolatier.engine.systems.events :refer [event-system
                                                       init-events-system]]
            [chocolatier.engine.systems.audio :refer [audio-system]]
            [chocolatier.engine.systems.debug :refer [debug-collision-system]]
            [chocolatier.engine.systems.movement :refer [movement-system]]
            [chocolatier.engine.systems.ai :refer [ai-system]]
            [chocolatier.engine.systems.replay :refer [replay-system]]
            [chocolatier.engine.components.animateable :refer [animate]]
            [chocolatier.engine.components.controllable :refer [react-to-input
                                                                include-input-state]]
            [chocolatier.engine.components.debuggable :refer [draw-collision-zone
                                                              include-moveable-state-and-stage]]
            [chocolatier.engine.components.moveable :refer [move]]
            [chocolatier.engine.components.ai :refer [behavior
                                                      include-player-and-moveable-state]]
            [chocolatier.entities.player :refer [create-player!]]
            [chocolatier.entities.enemy :refer [create-enemy!]]))


(defn init-state
  "Returns a hashmap of the game state"
  [renderer stage width height tilemap sample-library]
  (mk-game-state
   {}
   :default
   [:custom (fn [state]
              (assoc-in state [:game :rendering-engine]
                        {:renderer renderer :stage stage}))]
   [:custom init-events-system]
   ;; Initial tile map
   [:custom (mk-tiles-from-tilemap! renderer stage tilemap)]
   ;; Player 1 entity
   [:custom (create-player! stage :player1 20 20 0 0 20)]
   ;; Enemies
   [:custom (fn [state]
              (ces/iter-fns
               state
               (vec
                (for [i (range 25)]
                  #(create-enemy! % stage (keyword (gensym)) 20)))))]
   ;; A scene is collection of keys representing systems
   ;; that will be called in sequential order
   [:scene :default [:input
                     :user-input
                     :ai
                     :broad-collision
                     :narrow-collision
                     :collision-debug
                     :movement
                     :tiles
                     :replay
                     :animate
                     :audio
                     :render
                     :events]]
   ;; Global event system broadcaster
   [:system :events event-system]
   ;; Updates the user input from keyboard,
   ;; standalone system with no components
   [:system :input input-system]
   ;; React to user input
   [:system :user-input user-input-system :controllable]
   [:component :controllable
    ;; Calls react-to-input
    ;; with additional argument
    ;; for the current input
    ;; state
    [react-to-input {:args-fn include-input-state}]]
   ;; Draw tile map in background
   [:system :tiles tile-system]
   ;; Render system for drawing sprites
   [:system :render render-system]
   ;; Audio system for playing sounds
   [:system :audio (audio-system sample-library)]
   ;; Animation system for animating sprites
   [:system :animate animation-system :animateable]
   [:component :animateable animate]
   ;; Collision detection system
   [:system :broad-collision (broad-collision-system (/ width 20))]
   [:system :narrow-collision narrow-collision-system]
   [:system :collision-debug debug-collision-system :collision-debuggable]
   [:component :collision-debuggable
    [draw-collision-zone {:args-fn include-moveable-state-and-stage}]]
   [:system :movement movement-system :moveable]
   [:component :moveable move]
   [:system :ai ai-system :ai]
   [:component :ai
    [behavior {:args-fn include-player-and-moveable-state}]]
   ;; Replay game state on user input
   [:system :replay (replay-system 14 50)]))
