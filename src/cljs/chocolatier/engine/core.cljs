(ns chocolatier.engine.core
  (:require [chocolatier.engine.ces :as ces])
  (:require-macros [chocolatier.macros :refer [forloop local >> <<]]))


(defmulti mk-state
  "Returns a hashmap representing game state. Dispatches based on the
   keyword of the first item in an arguments vector.

   Example:
   (mk-state {} [:component :c1 [identity]])"
  (fn [state args] (first args)))

;; NOTE: we apply and partial so that we can maintain the araties of
;; the wrapped function and maintain that the state hashmap is the
;; first argument so we can use the threading macro easily
(defmethod mk-state :entity
  [state [_ uid components]]
  (ces/mk-entity state uid components))

(defmethod mk-state :entity-remove
  [state [_ uid]]
  (ces/rm-entity state uid))

(defmethod mk-state :component
  [state [_ & args]]
  (apply (partial ces/mk-component state) args))

(defmethod mk-state :system
  [state [_ & args]]
  (apply (partial ces/mk-system state) args))

(defmethod mk-state :scene
  [state [_ & args]]
  (apply (partial ces/mk-scene state) args))

(defmethod mk-state :current-scene
  [state [_ scene-id]]
  (ces/mk-current-scene state scene-id))

(defmethod mk-state :renderer
  [state [_ & args]]
  (apply (partial ces/mk-renderer state) args))

;; Anything labeled as custom is expected to be a function that takes
;; a single argument which is state hm
(defmethod mk-state :custom
  [state [_ f]]
  (f state))

(defn mk-game-state
  "Returns a hashmap of the game state from a spec of system components
   and entities. Ordering does not matter. See mk-scene/system/component/entity
   for full details on required arguments and options.

   Example usage:
   (mk-game-state {} [:scene :default [:s1]]
                     [:component :c1 f1]
                     [:system :s1 f2 :c1]
                     [:component :c2 [f3 {:args-fn f4}]]
                     [:entity :e1 :components [:c2]])"
  [state & specs]
  ;; After constructing the state, create a single arg update
  ;; function that is cached in :game :update-fns scene-id. This
  ;; provides some optimization since it means we don't have to
  ;; dynamically construct the update function every time.
  (let [new-state (reduce mk-state state specs)
        scene-id (get-in new-state ces/scene-id-path)
        systems (get-in new-state [:scenes scene-id])
        system-fns (ces/get-system-fns new-state systems)
        update-fn (apply comp (reverse system-fns))]
    (assoc-in new-state [:game :update-fns scene-id] update-fn)))

(defn timestamp
  "Get the current timestamp using performance.now.
   Fall back to Date.getTime for older browsers"
  []
  (if (and (exists? (aget js/window "performance"))
           (exists? (aget js/window "performance" "now")))
    (js/window.performance.now)
    ((aget (new js/Date) "getTime"))))

;; TODO this should be used as a fallback if requestAnimationFrame is
;; not available in this browser
(defn set-interval
  "Creates an interval loop of n calls to function f per second.
   Returns a function to cancel the interval."
  [f n]
  (.setInterval js/window f (/ 1000 n))
  #(.clearInterval f (/ 1000 n)))

(defn request-animation [f]
  (js/requestAnimationFrame f))

(defn next-state
  "Returns the next game state. The update function for the game is stored
   in the game state as an optimization."
  [game-state]
  (let [scene-id (get-in game-state ces/scene-id-path)
        update-fn (get-in game-state [:game :update-fns scene-id])]
    (update-fn game-state)))

(defn game-loop
  "Returns a game loop using requestAnimation to optimize frame rate. When
   the loop terminates, returns the last game state.

   Args:
   - game-state: The game state hash map. See mk-state for more.

   Optional Args:
   - middleware: A function that takes the next state function and returns
     a function that is called with a single argument for the current state.
     The inner function should return the next state or nil to indicate
     the game loop should terminate. This is similar to ring middleware.

     Example that logs the next game state:
     (defn wrap-log-state [f]
       (fn [state]
         (let [next-state (f state)]
           (println next-state)
           next-state)))"
  ([game-state]
   (request-animation #(game-loop (next-state game-state))))
  ([game-state middleware]
   (let [state ((middleware next-state) game-state)]
     (when state
       (request-animation #(game-loop state middleware))))))
