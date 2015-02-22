(ns chocolatier.engine.core
  (:require [chocolatier.engine.ces :as ces]))

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
   (mk-game-state {} [:component :c1 [f1]]
                     [:system :my-system f2 :c1]
                     [:component :c2 [[f3 {:args-fn f4}] f5]])"
  [state & specs]
  (reduce (fn [state args] (mk-state state args)) state specs))

