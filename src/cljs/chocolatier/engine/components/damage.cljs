(ns chocolatier.engine.components.damage
  (:require [clojure.string :as st]
            [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]
            [chocolatier.engine.components.moveable :refer [mk-moveable-state]]))


(defn mk-damage-state
  "Returns a hashmap of damage component state.
   - hitpoints: the maximum amount of damage before the entity is destroyed
   - defense: the resistance to damage from attacks"
  [hitpoints defense text-fn]
  {:hitpoints hitpoints :defense defense :text-fn text-fn})

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
  (let [;; TODO buffer the damage per attack so you only take n hits
        ;; in n frames
        {:keys [pos-x pos-y]} move-state
        text-fn (:text-fn component-state)
        damage (reduce + (map :damage attacks))
        next-component-state (update component-state :hitpoints - damage)
        destroy? (< (:hitpoints next-component-state) 1)
        msg [:entity (keyword (gensym "damage-"))
             [[:moveable (mk-moveable-state pos-x pos-y 5 :up-right)]
              [:ephemeral {:ttl 5 :counter 0}]
              [:text (text-fn (str "-" damage))]]]]
    ;; If hitpoints falls below 1 then remove entity from the game
    (if destroy?
      [next-component-state [(ev/mk-event [:entity-remove entity-id] [:meta])]]
      [next-component-state [(ev/mk-event msg [:meta])]])))

;; If there are any collisions with events from entity-ids that start
;; with :attack- then handle damage (action event to trigger
;; animation, update hitpoints, emit event to remove entity)
(defn damage
  [entity-id component-state {:keys [inbox move-state]}]
  (let [attacks (for [event inbox
                      collision (get-in event [:msg :collisions])
                      :let [collision-state (last collision)]
                      :when (valid-attack? entity-id collision-state)]
                  ;; Hashmap of :from-id :damage :type :position
                  (assoc (:attributes collision-state)
                         :position (take 2 collision)))]
    (if (seq attacks)
      (handle-damage entity-id component-state move-state attacks)
      component-state)))
