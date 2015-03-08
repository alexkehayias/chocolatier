(ns chocolatier.engine.core
  (:require [chocolatier.engine.ces :as ces])
  (:require-macros [chocolatier.macros :refer [forloop local >> <<]]))

(defmulti mk-state
  "Returns a hasmap representing game state. Dispatches based on the
   keyword of the first item in an arguments vector.

   Example:
   (mk-state {} [:component :c1 [identity]])"
  (fn [state args] (first args)))

;; NOTE: we apply and partial so that we can maintain the araties of
;; the wrapped function and maintain that the state hashmap is the
;; first argument so we can use the threading macro easily
(defmethod mk-state :entity
  [state [_ & args]]
  (apply (partial ces/mk-entity state) args))

(defmethod mk-state :component
  [state [_ & args]]
  (apply (partial ces/mk-component state) args))

(defmethod mk-state :system
  [state [_ & args]]
  (apply (partial ces/mk-system state) args))

(defmethod mk-state :scene
  [state [_ & args]]
  (apply (partial ces/mk-scene state) args))

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
   (mk-game-state {} :default
                     [:component :c1 [f1]]
                     [:system :my-system f2 :c1]
                     [:component :c2 [[f3 {:args-fn f4}] f5]])"
  [state init-scene-id & specs]
  (reduce (fn [accum args] (mk-state accum args))
          (assoc-in state [:game :scene-id] init-scene-id)
          specs))

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
  [game-state]
  ;; Uses transient state for efficiency
  (let [scene-id (get-in game-state [:game :scene-id])
        systems (ces/get-system-fns game-state (-> game-state :scenes scene-id))
        state (local game-state)]
    (forloop [[i 0] (< i (count systems)) (inc i)]
             (>> state ((systems i) (<< state))))
    ;; Copy the state into an atom so we can inspect while running
    (reset! *state* (<< state))
    (if @*running*
      (request-animation #(game-loop (<< state)))
      (println "Game stopped"))))
