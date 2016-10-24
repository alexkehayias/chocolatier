(ns chocolatier.engine.components.damage
  (:require [clojure.string :as st]
            [chocolatier.utils.logging :as log]
            [chocolatier.engine.utils.counters :as cnt]
            [chocolatier.engine.events :as ev]
            [chocolatier.engine.components.text :refer [mk-text-state]]
            [chocolatier.engine.components.moveable :refer [mk-moveable-state]]
            [chocolatier.engine.components.position :refer [mk-position-state]]
            [chocolatier.engine.components.ephemeral :refer [mk-ephemeral-state]]))


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
  [entity-id {:keys [from-id damage]}]
  (and (not (keyword-identical? entity-id from-id)) damage))

(defn handle-damage
  "Returns updated component state and collection of events.
   - Updates :hitpoints by calculating damage factoring in defense
   - If :hitpoints falls below 1 then the entity is destroyed
   - Creates an text entity with the amount of damage taken"
  [entity-id component-state position-state attacks]
  ;; TODO emit action events like hit animation
  (let [{:keys [screen-x screen-y screen-z]} position-state
        text-fn (:text-fn component-state)
        damage (transduce (map :damage) + 0 attacks)
        next-state (-> component-state
                       (update :hitpoints - damage)
                       (update :cooldown #(first (cnt/tick-cooldown %))))
        destroy? (< (:hitpoints next-state) 1)]
    ;; If hitpoints falls below 1 then remove entity from the game
    ;; otherwise create an entity with a text component to display the
    ;; damage taken
    (if ^boolean destroy?
      [next-state [(ev/mk-event {:type :entity-remove
                                 :opts {:uid entity-id}} [:meta])]]
      [next-state [(ev/mk-event {:type :entity
                                 :opts {:uid (keyword (gensym "damage-"))
                                        :components [{:uid :position :state (mk-position-state screen-x screen-y screen-x screen-y screen-z)}
                                                     {:uid :moveable :state (mk-moveable-state 2 :up)}

                                                     {:uid :ephemeral :state (mk-ephemeral-state 10)}
                                                     ;; WARNING: This function has side
                                                     ;; effects and should only be called
                                                     ;; when needed i.e not in a let
                                                     ;; binding if it could potentially
                                                     ;; not be used
                                                     {:uid :text-sprite :state (text-fn (str "-" damage))}
                                                     {:uid :text :state (mk-text-state (str "-" damage) 0.5)}]}}
                                [:meta])
                   ;; Emit a hit action for the entity
                   ;; (ev/mk-event {:action :hit :direction :up}
                   ;;              [:action entity-id])
                   ]])))

(defn tick-in-progress-damage
  [component-state]
  (if (cnt/cooldown? (:cooldown component-state))
    (update component-state :cooldown #(first (cnt/tick-cooldown %)))
    component-state))

(defn get-attacks
  "Returns a collection of valid attacks on the entity"
  [entity-id inbox]
  ;; Use a double nested loop to optimize performance
  (let [col-path [:msg :collisions]]
    (loop [events inbox
           accum (array)]
      (let [event (first events)]
        (if (nil? event)
          accum
          (let [collisions (get-in event col-path)
                next-accum (loop [collisions collisions
                                  i (array 0)
                                  acc accum]
                             (let [indx (aget i 0)
                                   collision (aget collisions indx)]
                               (if (nil? collision)
                                 acc
                                 (let [attributes (aget collision 4)]
                                   (recur collisions
                                          (do (aset i 0 (+ indx 1)) i)
                                          (if (valid-attack? entity-id attributes)
                                            (do (.push acc attributes) acc)
                                            acc))))))]
            (recur (rest events) next-accum)))))))

(defn damage
  "If there are any collisions with a valid attack and the entity is not
   invinsible (can only be attacked every n frames) then handle damage
   to the entity. If hitpoints falls below 0 then emit a message to
   remove the entity from the game-state."
  [entity-id component-state {:keys [inbox position]}]
  ;; Use a loop here for performance reasons
  (let [attacks (get-attacks entity-id inbox)
        vulnerable? (not (cnt/cooldown? (:cooldown component-state)))]
    (if ^boolean (and vulnerable? (seq attacks))
        (handle-damage entity-id component-state position attacks)
        (tick-in-progress-damage component-state))))
