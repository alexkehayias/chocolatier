(ns chocolatier.engine.components.damage
  (:require [clojure.string :as st]
            [chocolatier.utils.logging :as log]
            [chocolatier.engine.utils.counters :as cnt]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]
            [chocolatier.engine.components.moveable :refer [mk-moveable-state]]))


(defn mk-damage-state
  "Returns a hashmap of damage component state.
   - hitpoints: the maximum amount of damage before the entity is destroyed
   - defense: the resistance to damage from attacks
   - cooldown: the number of frames of invinsibility before getting being
     eligible to be hit again
   - text-fn: a fn that takes one argument and returns a text state using
     engine.components.renderable/mk-text-state"
  [hitpoints defense cooldown text-fn]
  {:hitpoints hitpoints
   :defense defense
   :text-fn text-fn
   :cooldown (cnt/mk-cooldown cooldown)})

(defn valid-attack?
  "Returns a boolean of whether the collision is a valid attack
   - Includes collisions that have an ID that start with :attack
   - Excludes collisions where the parent-id matches the entity-id
     (can't be damaged by yourself)"
  [entity-id {:keys [id attributes]}]
  (and (:damage attributes)
       (not= entity-id (:from-id attributes))))

(defn handle-damage
  "Returns updated component state and collection of events.
   - Updates :hitpoints by calculating damage factoring in defense
   - If :hitpoints falls below 1 then the entity is destroyed
   - Creates an text entity with the amount of damage taken"
  [entity-id component-state move-state attacks]
  ;; TODO emit action events like hit animation
  (let [{:keys [pos-x pos-y]} move-state
        text-fn (:text-fn component-state)
        damage (reduce + (map :damage attacks))
        next-state (-> component-state
                       (update :hitpoints - damage)
                       (update :cooldown #(first (cnt/tick-cooldown %))))
        destroy? (< (:hitpoints next-state) 1)
        msg [:entity (keyword (gensym "damage-"))
             [[:moveable (mk-moveable-state pos-x pos-y 2 :up)]
              [:ephemeral {:ttl 4 :counter 0}]
              [:text (text-fn (str "-" damage))]]]]
    ;; If hitpoints falls below 1 then remove entity from the game
    (if destroy?
      [next-state [(ev/mk-event [:entity-remove entity-id] [:meta])]]
      [next-state [(ev/mk-event msg [:meta])]])))

(defn tick-in-progress-damage
  [component-state]
  (if (cnt/cooldown? (:cooldown component-state))
    (update component-state :cooldown #(first (cnt/tick-cooldown %)))
    component-state))

(defn damage
  "If there are any collisions with a valid attack and the entity is not
   invinsible (can only be attacked every n frames) then handle damage
   to the entity. If hitpoints falls below 0 then emit a message to
   remove the entity from the game-state."
  [entity-id component-state {:keys [inbox move-state]}]
  (let [attacks (for [event inbox
                      collision (get-in event [:msg :collisions])
                      :let [collision-state (last collision)]
                      :when (valid-attack? entity-id collision-state)]
                  ;; Hashmap of :from-id :damage :type :position
                  (assoc (:attributes collision-state)
                         :position (take 2 collision)))
        vulnerable? (not (cnt/cooldown? (:cooldown component-state)))]
    (if (and vulnerable? (seq attacks))
      (handle-damage entity-id component-state move-state attacks)
      (tick-in-progress-damage component-state))))
