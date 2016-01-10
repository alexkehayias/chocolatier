(ns chocolatier.engine.core
  (:require [chocolatier.engine.ces :as ces])
  (:require-macros [chocolatier.macros :refer [forloop local >> <<]]))


(defn pmoc
  "Takes a collection of functions and returns a fn that is the composition
   of those fns. The returned function takes a single argument and
   applies the leftmost of fns to the arg, the next fn (left-to-right)
   to the result, etc."
  [fs]
  (fn [arg]
    (loop [ret ((first fs) arg) fs (next fs)]
      (if fs
        (recur ((first fs) ret) (next fs))
        ret))))

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
                     [:component :c1 [f1]]
                     [:system :s1 f2 :c1]
                     [:component :c2 [[f3 {:args-fn f4}] f5]]
                     [:entity :e1 :components [:c2] :subscriptions [[:e1 :ev1]]])"
  [state & specs]
  ;; After constructing the state, create a single arg update
  ;; function that is cached in :game :update-fns scene-id
  (let [new-state (reduce mk-state state specs)
        scene-id (get-in new-state ces/scene-id-path)
        systems (get-in new-state [:scenes scene-id])
        system-fns (ces/get-system-fns new-state systems)
        update-fn (pmoc system-fns)]
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

;; A copy of the game state goes here so that it can be inspected
;; any time while the game is running
(def *state* (atom nil))

;; Determines if the game is running, defaults to true
(def *running* (atom true))

(defn game-loop
  "Returns a game loop using requestAnimation to optimize frame rate.
   Temporarily stop the game by resetting the *running* atom. State is
   copied into the *state* atom to allow for inspection whilethe loop
   is running.

   Args:
   - state: the game state hash map"
  [game-state running?]
  (let [scene-id (get-in game-state ces/scene-id-path)
        update-fn (get-in game-state [:game :update-fns scene-id])
        next-state (update-fn game-state)]
    (if @running?
      (request-animation #(game-loop next-state running?))
      (println "Game stopped"))))

(defn game-loop-with-stats
  [game-state stats-obj]
  (.begin stats-obj)
  (let [scene-id (get-in game-state ces/scene-id-path)
        update-fn (get-in game-state [:game :update-fns scene-id])
        next-state (update-fn game-state)]
    ;; Copy the state into an atom so we can inspect while running
    (reset! *state* next-state)
    (.end stats-obj)
    (if @*running*
      (request-animation #(game-loop-with-stats next-state stats-obj))
      (println "Game stopped"))))
