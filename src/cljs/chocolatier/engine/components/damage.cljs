(ns chocolatier.engine.components.damage
  (:require [clojure.string :as st]
            [chocolatier.utils.logging :as log]
            [chocolatier.engine.utils.counters :as cnt]
            [chocolatier.engine.events :as ev]
            [chocolatier.engine.components.text :refer [mk-text-state]]
            [chocolatier.engine.components.moveable :refer [mk-moveable-state]]))


(defn mk-damage-state
  "Returns a hashmap of damage component state.
   - hitpoints: the maximum amount of damage before the entity is destroyed
   - defense: the resistance to damage from attacks
   - cooldown: the number of frames of invinsibility before getting being
     eligible to be hit again
   - text-fn: a fn that takes one argument and returns a text state using
     engine.components.renderable/mk-text-sprite-state"
  [hitpoints defense cooldown text-fn]
  {:hitpoints hitpoints
   :defense defense
   :text-fn text-fn
   :cooldown (cnt/mk-cooldown cooldown)})

(defn ^boolean valid-attack?
  "Returns a boolean of whether the collision is a valid attack
   - Includes collisions that have an ID that start with :attack
   - Excludes collisions where the parent-id matches the entity-id
     (can't be damaged by yourself)"
  [entity-id {:keys [id attributes]}]
  (and (not (keyword-identical? entity-id (:from-id attributes)))
       (:damage attributes)))

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
        destroy? (< (:hitpoints next-state) 1)]
    ;; If hitpoints falls below 1 then remove entity from the game
    ;; otherwise create an entity with a text component to display the
    ;; damage taken
    (if ^boolean destroy?
      [next-state [(ev/mk-event [:entity-remove entity-id] [:meta])]]
      [next-state [(ev/mk-event [:entity (keyword (gensym "damage-"))
                                 [[:moveable (mk-moveable-state pos-x pos-y 2 :up)]
                                  [:ephemeral {:ttl 4 :counter 0}]
                                  ;; WARNING: This function has side
                                  ;; effects and should only be called
                                  ;; when needed i.e not in a let
                                  ;; binding if it could potentially
                                  ;; not be used
                                  [:text-sprite (text-fn (str "-" damage))]
                                  [:text (mk-text-state (str "-" damage) 0.5)]]]
                                [:meta])]])))

(defn tick-in-progress-damage
  [component-state]
  (if ^boolean (cnt/cooldown? (:cooldown component-state))
    (update component-state :cooldown #(first (cnt/tick-cooldown %)))
    component-state))

(defn get-attacks
  "Returns a collection of valid attacks on the entity"
  [entity-id inbox]
  ;; Use a double nested loop to optimize performance
  (let [col-path [:msg :collisions]]
    (loop [events inbox
           accum (transient [])]
      (let [event (first events)]
        (if event
          (let [collisions (get-in event col-path)
                next-accum (loop [collisions collisions
                                  acc accum]
                             (let [collision (first collisions)]
                               (if collision
                                 (let [[pos-x pos-y _ _ collision-state] collision]
                                   (recur (rest collisions)
                                          (if (valid-attack? entity-id collision-state)
                                            ;; Append the position on
                                            ;; the attributes Hashmap
                                            ;; of :from-id :damage
                                            ;; :type :position
                                            (conj! acc (assoc (:attributes collision-state)
                                                              :position [pos-x pos-y]))
                                            acc)))
                                 acc)))]
            (recur (rest events) next-accum))
          (persistent! accum))))
    )
  )

(defn damage
  "If there are any collisions with a valid attack and the entity is not
   invinsible (can only be attacked every n frames) then handle damage
   to the entity. If hitpoints falls below 0 then emit a message to
   remove the entity from the game-state."
  [entity-id component-state {:keys [inbox moveable]}]
  ;; Use a loop here for performance reasons
  (let [attacks (get-attacks entity-id inbox)
        vulnerable? (not (cnt/cooldown? (:cooldown component-state)))]
    (if ^boolean (and vulnerable? (seq attacks))
        (handle-damage entity-id component-state moveable attacks)
        (tick-in-progress-damage component-state))))
