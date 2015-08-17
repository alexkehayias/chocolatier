(ns chocolatier.engine.components.debuggable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.pixi :as pixi]
            [chocolatier.engine.systems.events :as ev]))

(defn include-moveable-state-and-stage
  "Include the moveable component state and stage (for drawing new things adhoc)
   in the args passed to draw-collision-zone"
  [state component-id entity-id]
  (let [moveable-state (ces/get-component-state state :moveable entity-id)
        collidable-state (ces/get-component-state state :collidable entity-id)
        stage (get-in state [:game :rendering-engine :stage])]
    {:moveable-state moveable-state :collidable-state collidable-state :stage stage}))

(defn base-style!
  "Applies styles to the graphic."
  [graphic]
  (-> graphic
      (pixi/line-style 0)
      (pixi/fill 0xFFFF0B 0.3)))

(defn player-style!
  "Applies styles to the graphic."
  [graphic]
  (-> graphic
      (pixi/line-style 0)
      (pixi/fill 0x00FF0B 0.3)))

(defn collision-style!
  [graphic]
  (-> graphic
      (pixi/line-style 0)
      (pixi/fill 0xFF0000 0.3)))

(defmulti draw-collision-zone
  (fn [entity-id component-state opts]
    entity-id))

(defmethod draw-collision-zone :default
  [_ component-state {:keys [stage moveable-state collidable-state inbox]}]
  (let [{:keys [pos-x pos-y hit-radius height width]} moveable-state
        {:keys [hit-radius height width]} collidable-state
        ;; Center hitzone on middle of entity
        half-height (/ height 2)
        half-width (/ width 2)
        x (+ pos-x half-width)
        y (+ pos-y half-height)
        ;; Try to get the sprite for collision zone or create a new one
        graphic (or (:graphic component-state) (pixi/mk-graphic! stage))]
    ;; If there is a collision going on change set the color to red
    (if (seq (filter #(= (:event-id %) :collision) inbox))
      (-> graphic (pixi/clear) (collision-style!) (pixi/circle x y hit-radius))
      (-> graphic (pixi/clear) (base-style!) (pixi/circle x y hit-radius)))
    ;; If the sprite does not exist it will add it to component state
    (assoc component-state :graphic graphic)))

(defmethod draw-collision-zone :player1
  [_ component-state {:keys [stage moveable-state collidable-state inbox]}]
  (let [{:keys [pos-x pos-y]} moveable-state
        {:keys [hit-radius height width]} collidable-state
        ;; Center hitzone on middle of entity
        half-height (/ height 2)
        half-width (/ width 2)
        x (+ pos-x half-width)
        y (+ pos-y half-height)
        ;; Try to get the sprite for collision zone or create a new one
        graphic (or (:graphic component-state) (pixi/mk-graphic! stage))]
    ;; If there is a collision going on change set the color to red
    (if (seq (filter #(= (:event-id %) :collision) inbox))
      (-> graphic (pixi/clear) (collision-style!) (pixi/circle x y hit-radius))
      (-> graphic (pixi/clear) (player-style!) (pixi/circle x y hit-radius)))
    ;; If the sprite does not exist it will add it to component state
    (assoc component-state :graphic graphic)))
