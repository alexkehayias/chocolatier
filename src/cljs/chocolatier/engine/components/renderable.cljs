(ns chocolatier.engine.components.renderable
  (:require [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]
            [chocolatier.engine.pixi :as pixi]))


(defn include-moveable-animateable-state
  "State parsing function. Returns a map of moveable-state and animateable-state"
  [state component-id entity-id]
  {:moveable-state (ces/get-component-state state :moveable entity-id)
   :animateable-state (ces/get-component-state state :animateable entity-id)})

(defn include-moveable-text-state
  "State parsing function. Returns a map of with a key for moveable-state"
  [state component-id entity-id]
  {:moveable-state (ces/get-component-state state :moveable entity-id)
   :text-state (ces/get-component-state state :text entity-id)})

(defn cleanup-sprite-state
  "Removes sprite from the stage belonging to the entity and returns state"
  [state entity-id]
  (let [stage (-> state :game :rendering-engine :stage)
        {:keys [sprite] :as cs} (ces/get-component-state state :sprite entity-id)]
    (pixi/remove-child! stage sprite)
    state))

(defn cleanup-text-state
  "Removes sprite from the stage belonging to the entity and returns state"
  [state entity-id]
  (let [stage (-> state :game :rendering-engine :stage)
        {:keys [text-obj]} (ces/get-component-state state :text-sprite entity-id)]
    (pixi/remove-child! stage text-obj)
    state))

(defn set-position!
  "Update the screen x, y position of the sprite based on any move events
   from a component inbox. Returns the updated sprite."
  [sprite moveable-state]
  (let [{:keys [pos-x pos-y]} moveable-state]
    ;; Mutate the x and y position of the sprite if there was any
    ;; move changes
    (pixi/alter-obj! sprite "position" (js-obj "x" pos-x "y" pos-y))))

;; TODO figure out a way to not need the stage so we can more easily
;; create sprite state. For example, in the attack component we must
;; rely on a sprite-fn instead of constructing the sprite state inside
;; of the attack component fn
(defn mk-sprite-state
  "Returns a hashmap of render component state. Optionally pass in the
   sprite frame as the last argument to render to the position right away"
  ([stage sprite-sheet-file-name]
   {:sprite (pixi/mk-sprite! stage sprite-sheet-file-name)})
  ([stage sprite-sheet-file-name frame]
   {:sprite (pixi/mk-sprite! stage sprite-sheet-file-name frame)}))

(defn mk-text-sprite-state
  [stage text styles]
  {:text-obj (pixi/mk-text! stage text (clj->js styles))})

(defn render-sprite
  "Renders the sprite in relation to the position of the entity and
   frame of the spritesheet deterimined by the animateable state"
  [entity-id component-state {:keys [moveable-state animateable-state]}]
  (let [sprite (:sprite component-state)
        frame (:frame animateable-state)]
    ;; Side effects!
    ;; If there is a moveable state then update the position
    (when (seq moveable-state) (set-position! sprite moveable-state))
    ;; If there is an animation frame then update the spritesheet frame
    (when frame (pixi/set-sprite-frame! sprite frame))
    component-state))

(defn render-text
  "Renders text in relation to the position of the entity"
  [entity-id component-state {:keys [moveable-state text-state]}]
  (let [{:keys [text rotation]} text-state
        text-obj (:text-obj component-state)]
    ;; Mutate the text object position and text
    (when moveable-state (set-position! text-obj moveable-state))
    (pixi/alter-obj! text-obj "text" text "rotation" rotation)
    component-state))
