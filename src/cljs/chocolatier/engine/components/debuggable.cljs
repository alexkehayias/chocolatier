(ns chocolatier.engine.components.debuggable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.pixi :as pixi]))

(defn include-renderable-state
  "Include the renderable component state and stage (for drawing new things adhoc)
   in the args passed to draw-collision-zone"
  [state component-id entity-id]
  (let [renderable-state (ces/get-component-state state :renderable entity-id)
        component-state (ces/get-component-state state component-id entity-id)
        inbox (ces/get-event-inbox state component-id entity-id)]
    [stage component-state renderable-state component-id entity-id inbox]))

(defn base-style
  "Applies styles to the graphic."
  [graphic]
  (-> graphic
      (pixi/line-style 0)
      (pixi/fill 0xFFFF0B 0.5)))

(defmulti draw-collision-zone
  "Debug collision detection by drawing circles for the hitzone and turning red 
   when a collision message is received"
  (fn [stage component-state renderable-state component-id entity-id inbox]
    entity-id))

(defmethod draw-collision-zone :default
  [stage component-state renderable-state component-id entity-id inbox]
  (let [{:keys [pos-x pos-y hit-radius height width]} renderable-state
        ;; Try to get the sprite for collision zone or create a new one
        sprite (or (:sprite component-state)
                   (-> (pixi/mk-graphic! stage)
                       (pixi/add-to-stage stage)
                       (base-style)
                       (pixi/circle pos-x pos-y hit-radius)))
        ;; Center hitzone on middle of entity
        half-height (/ height 2) 
        half-width (/ width 2) 
        x (+ pos-x half-width)
        y (+ pos-y half-height)]
    ;; Mutate the x and y position
    (set! (.-position.x sprite) (:pos-x renderable))
    (set! (.-position.y sprite) (:pos-y renderable))
    ;; If the sprite does not exist it will add it to component state
    (assoc component-state :sprite sprite)))
