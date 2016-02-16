(ns chocolatier.examples.action-rpg.game
  (:require [chocolatier.engine.core :refer [mk-game-state]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.input :refer [input-system]]
            [chocolatier.engine.systems.user-input :refer [user-input-system]]
            [chocolatier.engine.systems.render :refer [render-system
                                                       sprite-system
                                                       text-sprite-system]]
            [chocolatier.engine.systems.animation :refer [animation-system]]
            [chocolatier.engine.systems.text :refer [text-system]]
            [chocolatier.engine.systems.collision :refer [mk-broad-collision-system
                                                          mk-narrow-collision-system]]
            [chocolatier.engine.systems.tiles :refer [tile-system load-tilemap mk-tiles-from-tilemap!]]
            [chocolatier.engine.systems.events :refer [event-system
                                                       init-events-system]]
            [chocolatier.engine.systems.audio :refer [audio-system]]
            [chocolatier.engine.systems.movement :refer [movement-system]]
            [chocolatier.engine.systems.ai :refer [ai-system]]
            [chocolatier.engine.systems.replay :refer [replay-system]]
            [chocolatier.engine.systems.combat :refer [attack-system
                                                       damage-system]]
            [chocolatier.engine.systems.ttl :refer [ttl-system]]
            [chocolatier.engine.systems.meta :refer [meta-system]]
            [chocolatier.engine.components.animateable :refer [animate]]
            [chocolatier.engine.components.text :refer [text]]
            [chocolatier.engine.components.renderable :refer [include-moveable-animateable-state
                                                              include-moveable-text-state
                                                              cleanup-sprite-state
                                                              cleanup-text-state
                                                              render-sprite
                                                              render-text]]
            [chocolatier.engine.components.controllable :refer [react-to-input
                                                                include-input-state]]
            [chocolatier.engine.components.moveable :refer [move]]
            [chocolatier.engine.components.ai :refer [behavior defer-events]]
            [chocolatier.engine.components.attack :refer [attack
                                                          include-move-state]]
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
   [:scene :default [:input
                     :user-input
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
   ;; Updates the user input from keyboard,
   ;; standalone system with no components
   [:system :input input-system]
   ;; Handles meta events like adding/removing entities by listening
   ;; to :meta events
   [:system :meta meta-system]
   ;; React to user input
   [:system :user-input user-input-system :controllable]
   [:component :controllable
    ;; Calls react-to-input with additional argument for the current
    ;; input state
    [react-to-input {:args-fn include-input-state}]]
   ;; Draw tile map in background
   [:system :tiles tile-system]
   ;; Render system for drawing sprites
   [:system :render render-system]
   ;; Audio system for playing sounds
   [:system :audio (audio-system sample-library)]
   ;; Sprite system for altering sprites
   [:system :sprite sprite-system :sprite]
   [:component :sprite [render-sprite {:args-fn include-moveable-animateable-state
                                       :cleanup-fn cleanup-sprite-state}] ]
   [:system :animate animation-system :animateable]
   [:component :animateable [animate {:subscriptions [:action]}]]
   ;; Text sprite system for displaying text
   [:system :text-sprite text-sprite-system :text-sprite]
   [:component :text-sprite [render-text {:args-fn include-moveable-text-state
                                          :cleanup-fn cleanup-text-state}] ]
   [:system :text text-system :text]
   [:component :text [text {:subscriptions [:text-change]}]]
   ;; Collision detection system
   [:system :broad-collision (mk-broad-collision-system 8)]
   [:system :narrow-collision (mk-narrow-collision-system height width)]
   [:system :attack attack-system :attack]
   [:component :attack
    [attack {:args-fn include-move-state
             :subscriptions [:action]}]]
   [:system :damage attack-system :damage]
   [:component :damage
    [damage {:args-fn include-move-state
             :subscriptions [:collision]}]]
   [:system :ttl ttl-system :ephemeral]
   [:component :ephemeral update-ttl]
   [:system :movement movement-system :moveable]
   [:component :moveable
    [move {:subscriptions [:move-change :collision]}]]
   [:system :ai ai-system :ai]
   [:component :ai [behavior {:format-fn defer-events}]]
   ;; Replay game state on user input
   [:system :replay (replay-system 14 50)]))
