(ns chocolatier.examples.action-rpg.game
  (:require [chocolatier.engine.core :refer [mk-game-state]]
            [chocolatier.engine.systems.input :refer [keyboard-input-system]]
            [chocolatier.engine.systems.render :refer [render-system]]
            [chocolatier.engine.systems.audio :refer [audio-system]]
            [chocolatier.engine.systems.replay :refer [replay-system]]
            [chocolatier.engine.systems.meta :refer [meta-system]]
            [chocolatier.engine.components.animateable :refer [animate]]
            [chocolatier.engine.components.text :refer [text]]
            [chocolatier.engine.components.moveable :refer [move]]
            [chocolatier.engine.components.ai :refer [behavior]]
            [chocolatier.engine.components.attack :refer [attack]]
            [chocolatier.engine.components.damage :refer [damage]]
            [chocolatier.engine.components.ephemeral :refer [update-ttl]]
            [chocolatier.engine.components.position :refer [position]]
            [chocolatier.entities.player :refer [create-player!]]
            [chocolatier.entities.enemy :refer [create-enemy!]]
            [chocolatier.engine.systems.collision :refer [mk-entity-collision-system
                                                          mk-tilemap-collision-system]]
            [chocolatier.engine.systems.tiles :refer [tile-system
                                                      load-tilemap
                                                      mk-tiles-from-tilemap!]]
            [chocolatier.engine.systems.events :refer [event-system
                                                       init-events-system]]
            [chocolatier.engine.components.renderable :refer [cleanup-sprite-state
                                                              cleanup-text-state
                                                              render-sprite
                                                              render-text]]
            [chocolatier.engine.components.controllable :refer [react-to-input
                                                                include-input-state]]))


(defn init-state
  "Returns a hashmap of the game state"
  [renderer stage width height tilemap loader sample-library]
  ;; Specifies, in one place, how everything fits together.
  ;; Order should not matter!
  (mk-game-state
   {}
   ;; Load the renderer object
   {:type :renderer :opts {:renderer renderer :stage stage}}
   ;; Initialize event system
   {:type :script :opts {:fn init-events-system}}
   ;; Initial tile map
   {:type :tilemap
    :opts {:renderer renderer
           :stage stage
           :loader loader
           :tilemap tilemap}}
   ;; Player 1 entity
   {:type :entity
    :opts (create-player! stage loader :player1 20 20 0 0)}
   ;; Script enemy creation
   ;; {:type :script
   ;;  :opts {:fn (fn [state]
   ;;               (reduce #(create-enemy! %1 stage loader (keyword (gensym "enemy ")))
   ;;                       state
   ;;                       (range 100)))}}
   ;; A scene is collection of keys representing systems
   ;; that will be called in sequential order
   {:type :scene
    :opts {:uid :default
           :systems [:keyboard-input
                     :controller
                     :entity-collision
                     :tilemap-collision
                     :ai
                     :movement
                     :position
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
                     :events]}}
   ;; Sets the current scene
   {:type :current-scene
    :opts {:uid :default}}
   ;; Global event system broadcaster
   {:type :system
    :opts {:uid :events
           :fn event-system}}
   ;; Updates the user input from keyboard
   {:type :system
    :opts {:uid :keyboard-input
           :fn keyboard-input-system}}
   ;; Provides a map/screen position
   {:type :system
    :opts {:uid :position
           :component {:uid :position
                       :fn position
                       :subscriptions [:position-change]}}}
   ;; Handles meta events like adding/removing entities by listening
   ;; to :meta events
   {:type :system :opts {:uid :meta :fn meta-system}}
   ;; React to user input
   {:type :system
    :opts {:uid :controller
           :component {:uid :controllable
                       :fn react-to-input
                       :select-components [:keyboard-input]}}}
   ;; Draw tile map in background
   {:type :system
    :opts {:uid :tiles :fn tile-system}}
   ;; Render system for drawing sprites
   {:type :system
    :opts {:uid :render :fn render-system}}
   ;; Audio system for playing sounds
   {:type :system
    :opts {:uid :audio :fn (audio-system sample-library)}}
   ;; Sprite system for altering sprites
   {:type :system
    :opts {:uid :sprite
           :component {:uid :sprite
                       :fn render-sprite
                       :select-components [:position :animateable]
                       :cleanup-fn cleanup-sprite-state}}}
   ;; Animation system for spritesheets
   {:type :system
    :opts {:uid :animate
           :component {:uid :animateable
                       :fn animate
                       :subscriptions [:action]}}}
   ;; Text sprite system for displaying text
   {:type :system
    :opts {:uid :text-sprite
           :component {:uid :text-sprite
                       :fn render-text
                       :select-components [:position :text]
                       :cleanup-fn cleanup-text-state}}}
   ;; The actual text to be displayed
   {:type :system
    :opts {:uid :text
           :component {:uid :text
                       :fn text
                       :subscriptions [:text-change]}}}
   ;; Collision detection system
   {:type :system
    :opts {:uid :entity-collision
           :fn (mk-entity-collision-system height width 16)}}
   ;; Collisions with the tile map
   {:type :system
    :opts {:uid :tilemap-collision
           :fn (mk-tilemap-collision-system height width 16)}}
   ;; System for making attacks
   {:type :system
    :opts {:uid :attack
           :component {:uid :attack
                       :fn attack
                       :select-components [:position]
                       :subscriptions [:action]}}}
   ;; System for taking damage from attacks
   {:type :system
    :opts {:uid :damage
           :component {:uid :damage
                       :fn damage
                       :select-components [:position]
                       :subscriptions [:collision]}}}
   ;; For adding and removing entities with limited lifetime
   {:type :system
    :opts {:uid :ttl
           :component {:uid :ephemeral
                       :fn update-ttl}}}
   ;; System for handling movement
   {:type :system
    :opts {:uid :movement
           :component {:uid :moveable
                       :fn move
                       :subscriptions [:move-change :collision]}}}
   ;; System for controlling the behavior of NPCs
   {:type :system
    :opts {:uid :ai
           :component {:uid :ai
                       :fn behavior
                       :select-components [:position [:position :player1]]}}}
   ;; Replay game state on user input
   {:type :system
    :opts {:uid :replay :fn (replay-system 14 50)}}))
