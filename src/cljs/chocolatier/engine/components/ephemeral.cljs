(ns chocolatier.engine.components.ephemeral
  (:require [chocolatier.engine.events :as ev]))


(defn mk-ephemeral-state [duration]
  {:ttl duration :counter 0})

(defn update-ttl
  "Returns update component state with an incremented counter. If the
   counter exceeds the duration key of the component state then emit an
   event to remove the entity from the game state"
  [entity-id component-state _]
  (let [{:keys [counter ttl]} component-state
        inc-counter (inc counter)]
    (if ^boolean (> inc-counter ttl)
      [component-state [(ev/mk-event {:type :entity-remove :opts {:uid entity-id}}
                                     [:meta])]]
      (assoc component-state :counter inc-counter))))
