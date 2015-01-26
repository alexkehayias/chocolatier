(ns chocolatier.engine.components.controllable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))


(defn include-input-state
  "State parsing function. Returns a vector of input-state, component-state
   component-id and entity-id"
  [state component-id entity-id]
  (let [input-state (-> state :game :input)
        component-state (ces/get-component-state state component-id entity-id)
        inbox (ev/get-subscribed-events state entity-id)]
    [input-state component-state component-id entity-id inbox]))

(def move-rate 4)

(def keycode->interaction
  {:W {:action :walk :direction :up :offset-y (* 1 move-rate)}
   :S {:action :walk :direction :down :offset-y (* -1 move-rate)}
   :A {:action :walk :direction :left :offset-x (* 1 move-rate)}   
   :D {:action :walk :direction :right :offset-x (* -1 move-rate)}
   :¿ {:action :attack}
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
    (if (empty? input-seq)
      out
      (let [[k v] (first input-seq)
            out (if (= v "on")
                  (if-let [interaction (k keycode->interaction)]
                    (into out interaction) out)
                  out)]
        (recur out (rest input-seq))))))

(defn react-to-input
  [input-state component-state component-id entity-id inbox]
  (let [prev-direction (or (:direction component-state) :down)
        {:keys [action direction offset-x offset-y]} (input->interaction input-state)
        _ (println action direction offset-x offset-y)
        ;; Default to standing
        action (if-not action :stand action)
        ;; Default to prev direction
        direction (if-not direction prev-direction direction)
        events (cond->
                []
                (= action :replay)
                (conj (ev/mk-event {:replay? true} :replay))
                (= action :attack)
                (conj (ev/mk-event {:action action :direction direction}
                                   :action entity-id))                
                (= action :walk)
                (concat [(ev/mk-event {:offset-x offset-x :offset-y offset-y}
                                      :move-change entity-id)
                         (ev/mk-event {:action action :direction direction}
                                      :action entity-id)])
                (= action :stand)
                (conj (ev/mk-event {:action action :direction direction}
                                   :action entity-id)))
        updated-state (assoc component-state :action action :direction direction)]
    (if (empty? events)
      updated-state
      [updated-state events])))
