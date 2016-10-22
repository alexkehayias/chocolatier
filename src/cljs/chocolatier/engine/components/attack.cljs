(ns chocolatier.engine.components.attack
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ecs :as ecs]
            [chocolatier.engine.events :as ev]
            [chocolatier.engine.components.collidable :refer [mk-collidable-state]]
            [chocolatier.engine.components.moveable :refer [mk-moveable-state]]
            [chocolatier.engine.components.position :refer [mk-position-state]]
            [chocolatier.engine.components.animateable :refer [mk-animateable-state]]
            [chocolatier.engine.components.ephemeral :refer [mk-ephemeral-state]]
            [chocolatier.engine.utils.counters :refer [mk-cooldown
                                                       tick-cooldown
                                                       cooldown?]]))


(defn mk-attack-state
  "Returns a hashmap of attack component-state.

   Usage:
   (mk-attack-state [[:punch {:damage 10
                              :cooldown 5
                              :speed 5
                              :ttl 8
                              :type :physical
                              :sprite-fn sprite-f}]])"
  [action-specs]
  (into {} (map (fn [[action spec]]
                  (assert (= (set [:damage :cooldown :type
                                   :width :height :speed
                                   :ttl :sprite-fn])
                             (set (keys spec)))
                          "Missing required attack metadata")
                  [action (update spec :cooldown mk-cooldown)])
                action-specs)))

(defn player-attack
  "Unless the attack specified by the event is currently in cooldown
   then emit an event to create the attack. Returns updated
   component-state and any events if necessary"
  [entity-id component-state event position]
  (let [{:keys [action direction]} (:msg event)
        {:keys [damage type width height ttl sprite-fn speed]} (get component-state action)
        {:keys [screen-x screen-y]} position
        cooldown (get-in component-state [action :cooldown])
        [cooldown-state cooldown?] (tick-cooldown cooldown)
        next-state (assoc-in component-state [action :cooldown] cooldown-state)]
    (if ^boolean cooldown?
      next-state
      (let [uid (keyword (gensym "attack-"))
            collision-state (mk-collidable-state width height
                                                 {:from-id entity-id
                                                  :damage damage
                                                  :type type})
            move-state (mk-moveable-state speed direction)
            position-state (mk-position-state (+ screen-x 16)
                                              (+ screen-y 24)
                                              (+ screen-x 16)
                                              (+ screen-y 24))
            ephemeral-state (mk-ephemeral-state ttl)
            sprite-state (sprite-fn)
            msg {:type :entity
                 :opts {:uid uid
                        :components [{:uid :collidable :state collision-state}
                                     ;; Determine where the sprite goes based
                                     ;; on the position of the player
                                     {:uid :moveable :state move-state}
                                     {:uid :position :state position-state}
                                     ;; Add a ttl to the attack entity so we
                                     ;; don't need to handle cleaning it up!
                                     {:uid :ephemeral :state ephemeral-state}
                                     ;; Add a sprite to visualize the attack
                                     ;; Sprite component state comes from
                                     ;; calling a function due to needing the stage
                                     {:uid :sprite :state sprite-state}]}}
            e (ev/mk-event msg [:meta])]
        [next-state [e]]))))

(defn enemy-attack
  [entity-id component-state event position]
  component-state)

(defn handle-attack
  [entity-id component-state event position]
  ;; Purposely avoiding multimethods here because they are slow
  (condp keyword-identical? entity-id
    :player1 (player-attack entity-id component-state event position)
    (enemy-attack entity-id component-state event position)))

(defn get-attack-event
  "Returns the first attack event from the inbox"
  [component-state inbox]
  ;; HACK This relies on the component state being keyed off of the
  ;; attack name which means that this is vulnerable to conflicting
  ;; with other action types
  (some #(when (contains? component-state (get-in % [:msg :action])) %)
        inbox))

(defn tick-in-progress-attack
  [component-state [action action-state]]
  (if ^boolean (cooldown? (:cooldown action-state))
    (update-in component-state [action :cooldown] #(first (tick-cooldown %)))
    component-state))

(defn attack
  "Handles making attackes for the enemy. Must subscribe to
   the :action events for the entity."
  [entity-id component-state {:keys [inbox position]}]
  (if-let [event (get-attack-event component-state inbox)]
    ;; Dispatch to the attack handlers based on entity-id
    (handle-attack entity-id component-state event position)
    ;; Tick any in progress attack cooldowns
    (reduce tick-in-progress-attack component-state (seq component-state))))
