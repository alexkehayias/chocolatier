(ns chocolatier.engine.components.attack
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.events :as ev]
            [chocolatier.engine.components.collidable :refer [mk-collidable-state]]
            [chocolatier.engine.components.moveable :refer [mk-moveable-state]]
            [chocolatier.engine.components.animateable :refer [mk-animateable-state]]
            [chocolatier.engine.utils.counters :refer [mk-cooldown
                                                       tick-cooldown
                                                       cooldown?]]))


(defn mk-attack-state
  "Returns a hashmap of attack component-state.

   Usage:
   (mk-attack-state [:punch {:damage 10
                             :cooldown 5
                             :speed 5
                             :ttl 8
                             :type :physical
                             :sprite-fn sprite-f}])"
  [& action-specs]
  (into {} (map (fn [[action spec]]
                  (assert (= (set [:damage :cooldown :type
                                   :width :height :speed
                                   :ttl :sprite-fn])
                             (set (keys spec)))
                          "Missing required attack metadata")
                  [action (update spec :cooldown mk-cooldown)])
                action-specs)))

(defn include-move-state
  "Returns a map move state of the entity"
  [state component-id entity-id]
  {:move-state (ces/get-component-state state :moveable entity-id)})

(defn player-attack
  "Unless the attack specified by the event is currently in cooldown
   then emit an event to create the attack. Returns updated
   component-state and any events if necessary"
  [entity-id component-state event move-state]
  (let [{:keys [action direction]} (:msg event)
        {:keys [damage type width height ttl sprite-fn speed]} (get component-state action)
        {:keys [pos-x pos-y]} move-state
        cooldown (get-in component-state [action :cooldown])
        [cooldown-state cooldown?] (tick-cooldown cooldown)
        next-state (assoc-in component-state [action :cooldown] cooldown-state)]
    (if cooldown?
      next-state
      (let [uid (keyword (gensym "attack-"))
            msg [:entity
                 uid
                 [[:collidable (mk-collidable-state width height
                                                    {:from-id entity-id
                                                     :damage damage
                                                     :type type})]
                  ;; Determine where the sprite goes based
                  ;; on the position of the player
                  [:moveable (mk-moveable-state (+ pos-x 16)
                                                (+ pos-y 24)
                                                speed
                                                direction)]
                  ;; Add a ttl to the attack entity so we
                  ;; don't need to handle cleaning it up!
                  [:ephemeral {:ttl ttl :counter 0}]
                  ;; Add a sprite to visualize the attack
                  ;; Sprite component state comes from
                  ;; calling a function due to needing the stage
                  [:sprite (sprite-fn)]]]
            e (ev/mk-event msg [:meta])]
        [next-state [e]]))))

(defn enemy-attack
  [entity-id component-state event move-state]
  component-state)

(defn handle-attack
  [entity-id component-state event move-state]
  ;; Purposely avoiding multimethods here because they are slow
  (condp keyword-identical? entity-id
    :player1 (player-attack entity-id component-state event move-state)
    (enemy-attack entity-id component-state event move-state)))

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
  (if (cooldown? (:cooldown action-state))
    (update-in component-state [action :cooldown] #(first (tick-cooldown %)))
    component-state))

(defn attack
  "Handles making attackes for the enemy. Must subscribe to
   the :action events for the entity."
  [entity-id component-state {:keys [inbox move-state]}]
  (if-let [event (get-attack-event component-state inbox)]
    ;; Dispatch to the attack handlers based on entity-id
    (handle-attack entity-id component-state event move-state)
    ;; Tick any in progress attack cooldowns
    (reduce tick-in-progress-attack component-state (seq component-state))))
