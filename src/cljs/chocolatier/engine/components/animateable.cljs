(ns chocolatier.engine.components.animateable
  (:require [chocolatier.engine.ces :as ces]
            [chocolatier.engine.pixi :as pixi]))


(defn get-sprite-coords
  "Returns a pair of values for the x and y position of a sprite at frame-n"
  [w h frame-w frame-h col-w frame-n]
  (let [row (js/Math.floor (/ frame-n col-w))
        col (if (> (inc frame-n) col-w)
              (- frame-n (* row col-w))
              frame-n)
        x (* col frame-h)
        y (* row frame-w)]
    [x y]))

(defn mk-animation-fn
  "Returns a function that when called with a sprite object and frame number:
   - Alters a sprite to the desired frame number and returns updated sprite
   - If the frame number called is greater than the animation frame-count,
     returns nil

   Args:
   - sprite-w: Width of the spritesheet in pixels
   - sprite-h: Height of the spritesheet in pixels
   - frame-w: Width of the frame in pixels
   - frame-h: Height of the frame in pixels
   - sprite-row: Which spritesheet row begin the animation sequence on
   - sprite-col: Which spritesheet column to start animation sequence on
   - frame-count: Number of frames long the animation is"
  [sprite-w sprite-h frame-w frame-h sprite-row sprite-col frame-count]
  ;; Calculate the commonly used math in the returned function's
  ;; closure so it does not need to be calculated on every frame
  (let [col-w (js/Math.floor (/ sprite-w frame-w))
        frame-offset (+ (* col-w sprite-row) sprite-col)]
    (fn [sprite frame-n]
      ;; NOTE frame-n is zero indezed so to compare to the total
      ;; frame-count correctly we must increment the frame-n by 1
      (when (<= (inc frame-n) frame-count)
        (let [;; Calculate the spritesheet frame number based on offset
              sprite-frame-n (+ frame-n frame-offset)
              [x y] (get-sprite-coords sprite-w
                                       sprite-h
                                       frame-w
                                       frame-h
                                       col-w
                                       sprite-frame-n)]
          (pixi/set-sprite-frame! sprite x y frame-w frame-h))))))

(defn mk-animation
  "Returns a hashmap of the name and function to call to animate"
  [kw
   spritesheet-w spritesheet-h
   frame-w frame-h
   sprite-row sprite-col
   frame-count]
  {kw (mk-animation-fn spritesheet-w
                       spritesheet-h
                       frame-w
                       frame-h
                       sprite-row
                       sprite-col
                       frame-count)})

(defn mk-animations-map
  "Returns a hashmap of animations based on a collection of animation-specs.

   Animation spec:
   [kw                           ;; Unique keyword name of the animation
    spritesheet-w spritesheet-h  ;; Width and height of the spritesheet
    frame-w frame-h              ;; Width and height of a frame
    sprite-row sprite-col        ;; Offset row and column to start animation
    frame-count]                 ;; Number of frames in the animation

   Example:
   (mk-animations-map [:walk 10 10 2 2 0 0 5]
                      [:run 10 10 2 2 1 0 5])"
  [& animation-specs]
  (apply merge (map #(apply mk-animation %) animation-specs)))

(defn -mk-animation-state
  "Helper function for constructing state for animating sprites based 
   on a spritesheet.

   Example:
   (-mk-animation-state \"static/images/my-spritesheet.png\" 
                       0 0
                       :walk
                       [:walk 10 10 2 2 0 0 5]
                       [:run 10 10 2 2 1 0 5])"
  [stage
   image-location
   screen-x screen-y
   default-animation-kw
   & animation-specs]
  (let [sprite (-> (pixi/mk-sprite! stage image-location)
                   (pixi/alter-obj! "position.x" screen-x "position.y" screen-y)
                   ;; Initialized at 0, on the first frame it will use
                   ;; the animation specs to set it correctly
                   (pixi/set-sprite-frame! 0 0 0 0))
        animations (apply mk-animations-map animation-specs)]
    ;; Set the initial sprite position    
    (set! (.-position.x sprite) screen-x)
    (set! (.-position.y sprite) screen-y)    
    {:animation-stack (list default-animation-kw)
     :sprite sprite
     :animations animations
     :frame 0}))

(defn mk-animateable-state
  "Returns a hashmap of updated state with all required animation 
   component state"
  [state stage entity-id image-location x y default-animation & animation-specs]
  (ces/mk-component-state
   state
   :animateable
   entity-id
   (apply (partial -mk-animation-state stage image-location x y default-animation)
          animation-specs)))

(defn incr-frame
  "Increments the frame of a sprite's spritesheet. If the animation sequence is
   at the end, starts from the beginning. Returns a pair; updated sprite and frame."
  [sprite animation-fn frame-n]
  (if-let [updated-sprite (animation-fn sprite (inc frame-n))] 
    [updated-sprite (inc frame-n)]
    [(animation-fn sprite 0) 0]))

(defn get-move
  "Returns a collection of :move events from an inbox."
  [inbox]
  (:msg (first (filter #(= (:event-id %) :move) inbox))))

(defn get-action
  "Returns a keyword of the first action event from the inbox and appends 
   the direction"
  [inbox]
  (when-let [event (first (filter #(= (:event-id %) :action) inbox))]
    (let [{:keys [action direction]} (:msg event)]
      (keyword (str (name action) "-" (name direction))))))

(defn update-coords
  "Update the screen x, y position of the sprite based on any move events
   from a component inbox. Returns the updated sprite."
  [sprite inbox]
  (when-let [{:keys [pos-x pos-y]} (get-move inbox)]
    ;; Mutate the x and y position of the sprite if there was any
    ;; move changes
    (set! (.-position.x sprite) pos-x)
    (set! (.-position.y sprite) pos-y))
  sprite)

;; WARNING: Assumes a single spritesheet and a single sprite
(defn animate
  "When an action event is in the inbox changes the state and switches 
   animations. Otherwise, the animation frame is incremented to the next 
   frame as specified by the animation spec.

   NOTE: The animation-stack must be a list not a vec so conj works as expected"
  [entity-id component-state inbox]
  (let [{:keys [animation-stack sprite frame animations]} component-state
        current-animation-name (first animation-stack)
        new-action (get-action inbox)
        animation-change? (not= new-action current-animation-name)
        frame-n (if animation-change? 0 frame)
        animation-name (or new-action current-animation-name)
        animation-fn (animation-name animations)
        _ (assert animation-fn (str "Animation " animation-name " not found"))
        [sprite frame] (-> sprite
                           ;; Apply screen changes due to movement
                           (update-coords inbox)
                           ;; Update the animation to next frame
                           (incr-frame animation-fn frame-n))]
    ;; Sticking this in a conditional to avoid doing extra work if
    ;; there wasn't an animation change
    (if-not animation-change?
      (assoc component-state :frame frame :sprite sprite)
      (assoc component-state
        :animation-stack (if animation-change?
                           (if new-action
                             (conj (drop 1 animation-stack) new-action)
                             ;; There always needs to be an animation
                             ;; so if the stack has only 1 item in it
                             ;; then do not drop any items
                             (if (> (count animation-stack) 1)
                               (drop 1 animation-stack)
                               animation-stack))
                           animation-stack)
        :frame frame
        :sprite sprite))))
