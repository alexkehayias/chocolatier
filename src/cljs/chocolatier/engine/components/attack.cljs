(ns chocolatier.engine.components.attack
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]
            [chocolatier.engine.components.collidable
             :refer [mk-collidable-state]]
            [chocolatier.engine.components.moveable
             :refer [mk-moveable-state]]
            [chocolatier.engine.components.animateable
             :refer [mk-animateable-state]]))


(defn mk-attack-state
  ;; Shape
  ;; {:strong-attack {:cooldown 10 :damage 10}}
  [action-specs]
  ;; TODO convert attack specs to something similar to how the
  ;; animation state works
  {})

(defn include-move-state-and-stage
  "Returns a map of rendering-engine stage and move state of the entity"
  [state component-id entity-id]
  {:stage (-> state :game :rendering-engine :stage)
   :move-state (ces/get-component-state state :moveable entity-id)})

(defn cooldown?
  "Returns true if the action is currently on cooldown"
  [action action-spec component-state]
  (< (:cooldown action-spec) (:counter component-state)))

(defn tick-cooldown
  "Increment cooldown for the action and reset to 0 if cooldown
   counter exceeds the limit. Returns updated component-state"
  [action component-state]
  (let [{:keys [counter cooldown]
         :or {counter 0 cooldown 8}} (get component-state action)
         inc-counter (inc counter)]
    ;; If it's greater than the max cooldown, reset to zero
    (cond
      (> inc-counter cooldown)
      [(assoc-in component-state [action :counter] 0) true]
      (zero? counter)
      [(assoc-in component-state [action :counter] inc-counter) false]
      :else
      [(assoc-in component-state [action :counter] inc-counter) true])))

(defn direction->offset
  [direction]
  (condp keyword-identical? direction
    :up [0 10]
    :down [0 -10]
    :left [10 0]
    :right [-10 0]))

(defn player-attack
  "Unless the attack specified by the event is currently in cooldown
   then emit an event to create the attack. Returns updated
   component-state and any events if necessary"
  [entity-id component-state event stage move-state]
  (let [{:keys [action direction]} (:msg event)
        [offset-x offset-y] (direction->offset direction)
        {:keys [pos-x pos-y]} move-state
        [next-component-state cooldown?] (tick-cooldown action component-state)]
    (if cooldown?
      next-component-state
      (let [uid (keyword (gensym "attack-"))
            e (ev/mk-event [:entity
                            uid
                            ;; TODO replace this width and height of
                            ;; the hit box in the attack spec
                            [[:collidable (mk-collidable-state 30 30
                                                               {:from-id entity-id
                                                                :damage 10
                                                                :type :fire})]
                             ;; Determine where the sprite goes based
                             ;; on the position of the player
                             [:moveable (assoc (mk-moveable-state (+ pos-x 16)
                                                                  (+ pos-y 24))
                                               :offset-x offset-x :offset-y offset-y)]
                             ;; Add a ttl to the attack entity so we
                             ;; don't need to handle cleaning it up!
                             [:ephemeral {:ttl 100 :counter 0}]
                             ;; Add a sprite animation
                             [:animateable (mk-animateable-state
                                            stage
                                            "img/fireball.png"
                                            pos-x pos-y
                                            :standing
                                            [:standing 30 30 30 30 0 0 1])]]]
                           [:meta])]
        (println "ATTACKED" entity-id)
        [next-component-state [e]]))))

(defn enemy-attack
  [entity-id component-state event stage move-state]
  component-state)

(defn handle-attack
  [entity-id component-state event stage move-state]
  ;; Purposely avoiding multimethods here because they are slow
  (condp keyword-identical? entity-id
    :player1 (player-attack entity-id component-state event stage move-state)
    (enemy-attack entity-id component-state event stage move-state)))

(defn get-attack-event
  "Returns the first attack event from the inbox"
  [inbox]
  (some #(when (= (get-in % [:msg :action]) :attack) %) inbox))

(defn tick-in-progress-attack
  [component-state [action action-state]]
  (if (pos? (get action-state :counter 0))
    (first (tick-cooldown action component-state))
    component-state))

(defn attack
  "Handles making attackes for the enemy. Must subscribe to
   the :action events for the entity."
  [entity-id component-state {:keys [inbox stage move-state]}]
  (if-let [event (get-attack-event inbox)]
    (handle-attack entity-id component-state event stage move-state)
    ;; Tick any in progress attack cooldowns
    (reduce tick-in-progress-attack component-state (seq component-state))))
