(ns chocolatier.engine.components.controllable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))


(defn include-input-state
  "State parsing function. Returns a vector of input-state, component-state
   component-id and entity-id"
  [state component-id entity-id]
  {:input-state (get-in state [:game :input])})

(def move-rate 4)

(def keycode->interaction
  {:W {:action :walk :direction :up :offset-x 0 :offset-y (* 1 move-rate)}
   :S {:action :walk :direction :down :offset-x 0 :offset-y (* -1 move-rate)}
   :A {:action :walk :direction :left :offset-x (* 1 move-rate) :offset-y 0}
   :D {:action :walk :direction :right :offset-x (* -1 move-rate) :offset-y 0}
   ;; TODO this causes a compiler error with optimizations advanced
   ;; :¿ {:action :attack}
   :B {:action :replay}})

;; FIX The output of the interaction hashmap is non-deterministic
;; because it is iterating through a hashmap where ordering is not
;; guaranteed. Need to iterate through only the accepted keycodes and
;; check if the input-state shows the key is "on". That way order is
;; controlled by the caller
(defn input->interaction
  "Returns a hashmap of the intended interactions based on user input.
   There can be only one direction and action, last pressed key wins.
   Keys:
   - offset-x/y: movement x/y in pixels
   - direction: direction the player is facing
   - action: the action based on the input"
  [input-state]
  (loop [out {}, input-seq (seq input-state)]
    (if (seq input-seq)
      (let [[k v] (first input-seq)
            out (if-let [interaction (k keycode->interaction)]
                  (into out interaction)
                  out)]
        (recur out (rest input-seq)))
      out)))

(defn react-to-input
  [entity-id component-state {:keys [input-state]}]
  (let [interaction-state (input->interaction input-state)]
    ;; If the old interaction state is the same as the new
    ;; interaction or there is no interaction then no need to do
    ;; anything
    (if (= component-state interaction-state)
      component-state
      (let [{:keys [action direction offset-x offset-y]} interaction-state
            prev-direction (or (:direction component-state) :down)
            ;; Default to standing
            action (if-not action :stand action)
            ;; Default to prev direction
            direction (if-not direction prev-direction direction)
            events (cond->
                       []
                     (= action :replay)
                     (conj (ev/mk-event {:replay? true} [:replay]))
                     (= action :attack)
                     (conj (ev/mk-event {:action action :direction direction}
                                        [:action entity-id]))
                     (= action :walk)
                     (into [(ev/mk-event {:offset-x offset-x :offset-y offset-y}
                                         [:move-change entity-id])
                            (ev/mk-event {:action action :direction direction}
                                         [:action entity-id])])
                     (= action :stand)
                     (into [(ev/mk-event {:offset-x 0 :offset-y 0}
                                         [:move-change entity-id])
                            (ev/mk-event {:action action :direction direction}
                                         [:action entity-id])]))
            updated-state {:action action
                           :direction direction
                           :offset-x offset-x
                           :offset-y offset-y}]
        (if (seq events)
          [updated-state events]
          updated-state)))))
