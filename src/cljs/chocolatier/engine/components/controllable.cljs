(ns chocolatier.engine.components.controllable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.events :as ev]))


(defn include-input-state
  "State parsing function. Returns a vector of input-state, component-state
   component-id and entity-id"
  [state component-id entity-id]
  {:input-state (get-in state [:game :input])})

(def move-rate 4)

(def keycode->movement
  {:W {:action :walk :direction :up}
   :S {:action :walk :direction :down}
   :A {:action :walk :direction :left}
   :D {:action :walk :direction :right}})

(def keycode->action
  {(keyword "¿") {:action :fireball}
   :H {:action :spear}
   :B {:action :replay}})

(def directions
  (set (remove nil? (map :direction (vals keycode->movement)))))

(def actions
  (set (remove nil? (map :action (vals keycode->action)))))

(def directions->multi-directions
  {#{:up :left} :up-left
   #{:up :right} :up-right
   #{:down :left} :down-left
   #{:down :right} :down-right})

(defn ^boolean direction? [v]
  (contains? directions v))

(defn ^boolean action? [v]
  (contains? actions v))

(defn comp-movement
  [v1 v2]
  (cond
    (direction? v1) (get directions->multi-directions #{v1 v2} v1)
    ;; Fireball takes precedence
    (action? v1) (if (or (keyword-identical? v1 :fireball)
                         (keyword-identical? v2 :fireball))
                   :fireball
                   v2)
    :else v2))

;; FIX The output of the interaction hashmap is non-deterministic
;; because it is iterating through a hashmap where ordering is not
;; guaranteed. Need to iterate through only the accepted keycodes and
;; check if the input-state shows the key is "on". That way order is
;; controlled by the caller
(defn input->move
  "Returns a hashmap of the intended interactions based on user input.
   There can be only one direction and action, last pressed key wins.
   Keys:
   - direction: direction the player is facing
   - action: the action based on the input"
  [input-state]
  (loop [out {}, input-seq (seq input-state)]
    (if (seq input-seq)
      (let [[k v] (first input-seq)
            next-out (if-let [interaction (k keycode->movement)]
                       (merge-with comp-movement out interaction)
                       out)]
        (recur next-out (rest input-seq)))
      out)))

(defn input->attack
  "Returns the first attack from the input state."
  ;; FIX this is non-deterministic
  [input-state]
  (some keycode->action (keys input-state)))

(defn react-to-input
  [entity-id component-state {:keys [keyboard-input]}]
  (let [last-input-state (:input-state component-state)
        move-action (input->move keyboard-input)
        attack-action (input->attack keyboard-input)
        {last-move-action :move-action
         last-attack-action :attack-action} component-state]
    ;; If the old interaction state is the same as the new
    ;; interaction or there is no interaction then no need to do
    ;; anything
    (if-not (identical? last-input-state keyboard-input)
      (let [{move :action
             direction :direction} move-action
             {attack :action} attack-action
             prev-direction (:direction component-state :down)
             ;; Default to standing
             move (or move :stand)
             ;; Default to prev direction
             direction (or direction prev-direction)
             ;; The events pipeline is in two stages, one for moving
             ;; the other for attacking
             events (cond-> []
                      (not= last-move-action move-action)
                      (cond-> (keyword-identical? move :walk)
                        (into [(ev/mk-event {:direction direction}
                                            [:move-change entity-id])
                               (ev/mk-event {:action move :direction direction}
                                            [:action entity-id])])
                        (keyword-identical? move :stand)
                        (into [(ev/mk-event {:direction :none}
                                            [:move-change entity-id])
                               (ev/mk-event {:action move :direction direction}
                                            [:action entity-id])]))
                      (not= last-attack-action attack-action)
                      (cond-> (keyword-identical? attack :replay)
                        (conj (ev/mk-event {:replay? true} [:replay]))
                        (keyword-identical? attack :fireball)
                        (conj (ev/mk-event {:action attack :direction direction}
                                           [:action entity-id]))
                        (keyword-identical? attack :spear)
                        (conj (ev/mk-event {:action attack :direction direction}
                                           [:action entity-id]))
                        ;; If there was a last attack, but not one
                        ;; now, then default to the move action. This
                        ;; prevents "ice skating" after an attack
                        ;; while moving
                        (and (seq last-attack-action) (not (seq attack-action)))
                        (conj (ev/mk-event {:action move :direction direction}
                                           [:action entity-id]))))
             next-state {:direction direction
                         :input-state keyboard-input
                         :move-action move-action
                         :attack-action attack-action}]
        (if (seq events)
          [next-state events]
          next-state))
      ;; No-op
      component-state )))
