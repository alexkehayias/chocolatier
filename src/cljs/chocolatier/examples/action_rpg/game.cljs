(ns chocolatier.examples.action-rpg.game
  (:require [chocolatier.engine.core :refer [mk-game-state]]
            [chocolatier.engine.systems.input :refer [keyboard-input-system]]
            [chocolatier.engine.systems.render :refer [render-system]]
            [chocolatier.engine.systems.collision
             :refer [mk-broad-collision-system mk-narrow-collision-system]]
            [chocolatier.engine.systems.tiles :refer [tile-system
                                                      load-tilemap
                                                      mk-tiles-from-tilemap!]]
            [chocolatier.engine.systems.events :refer [event-system
                                                       init-events-system]]
            [chocolatier.engine.systems.audio :refer [audio-system]]
            [chocolatier.engine.systems.replay :refer [replay-system]]
            [chocolatier.engine.systems.meta :refer [meta-system]]
            [chocolatier.engine.components.animateable :refer [animate]]
            [chocolatier.engine.components.text :refer [text]]
            [chocolatier.engine.components.renderable
             :refer [cleanup-sprite-state
                     cleanup-text-state
                     render-sprite
                     render-text]]
            [chocolatier.engine.components.controllable
             :refer [react-to-input include-input-state]]
            [chocolatier.engine.components.moveable :refer [move]]
            [chocolatier.engine.components.ai :refer [behavior]]
            [chocolatier.engine.components.attack :refer [attack]]
            [chocolatier.engine.components.damage :refer [damage]]
            [chocolatier.engine.components.ephemeral :refer [update-ttl]]
            [chocolatier.entities.player :refer [create-player!]]
            [chocolatier.entities.enemy :refer [create-enemy!]]))


(defn init-state
  "Returns a hashmap of the game state"
  [renderer stage width height tilemap sample-library]
  (mk-game-state
   {}
   [:renderer renderer stage]
   [:custom init-events-system]
   ;; Initial tile map
   [:custom (mk-tiles-from-tilemap! renderer stage tilemap)]
   ;; Player 1 entity
   [:custom (create-player! stage :player1 20 20 0 0)]
   ;; Enemies
   [:custom (fn [state]
              (reduce #(create-enemy! %1 stage (keyword (gensym)))
                      state
                      (range 100)))]
   ;; A scene is collection of keys representing systems
   ;; that will be called in sequential order
   [:scene :default [:keyboard-input
                     :controller
                     :ai
                     :broad-collision
                     :narrow-collision
                     :movement
                     :attack
                     :damage
                     :ttl
                     :tiles
                     :replay
                     :meta
                     :animate
                     :sprite
                     :text
                     :text-sprite
                     :audio
                     :render
                     :events]]
   [:current-scene :default]
   ;; Global event system broadcaster
   [:system :events event-system]
   ;; Updates the user input from keyboard
   [:system :keyboard-input keyboard-input-system]
   ;; Handles meta events like adding/removing entities by listening
   ;; to :meta events
   [:system :meta meta-system]
   ;; React to user input
   [:system :controller :controllable
    [react-to-input {:select-components [:keyboard-input]}]]
   ;; Draw tile map in background
   [:system :tiles tile-system]
   ;; Render system for drawing sprites
   [:system :render render-system]
   ;; Audio system for playing sounds
   [:system :audio (audio-system sample-library)]
   ;; Sprite system for altering sprites
   [:system :sprite
    :sprite [render-sprite {:select-components [:moveable :animateable]
                            :cleanup-fn cleanup-sprite-state}]]
   [:system :animate :animateable [animate {:subscriptions [:action]}]]
   ;; Text sprite system for displaying text
   [:system :text-sprite
    :text-sprite [render-text {:select-components [:text :moveable]
                               :cleanup-fn cleanup-text-state}]]
   [:system :text
    :text [text {:subscriptions [:text-change]}]]
   ;; Collision detection system
   [:system :broad-collision (mk-broad-collision-system 8)]
   [:system :narrow-collision (mk-narrow-collision-system height width)]
   [:system :attack :attack [attack {:select-components [:moveable]
                                     :subscriptions [:action]}]]
   [:system :damage :damage [damage {:select-components [:moveable]
                                     :subscriptions [:collision]}]]
   [:system :ttl :ephemeral update-ttl]
   [:system :movement :moveable [move {:subscriptions [:move-change :collision]}]]
   [:system :ai :ai [behavior {:select-components [:moveable [:moveable :player1]]}]]
   ;; Replay game state on user input
   [:system :replay (replay-system 14 50)]))
