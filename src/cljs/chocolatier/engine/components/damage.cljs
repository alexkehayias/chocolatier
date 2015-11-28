(ns chocolatier.engine.components.damage
  (:require [clojure.string :as st]
            [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))


(defn mk-damage-state
  "Returns a hashmap of damage component state.
   - hitpoints: the maximum amount of damage before the entity is destroyed
   - defense: the resistance to damage from attacks"
  [hitpoints defense]
  {:hitpoints hitpoints :defense defense})

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
   - If :hitpoints falls below 1 then the entity is destroyed"
  [entity-id component-state attacks]
  ;; TODO emit action events like hit animation
  ;; TODO emit events for damage taken to text rendering
  (let [;; TODO buffer the damage per attack so you only take n hits
        ;; in n frames
        damage (reduce + (map :damage attacks))
        next-component-state (update component-state :hitpoints - damage)
        destroy? (< (:hitpoints next-component-state) 1)]
    ;; If hitpoints falls below 1 then remove entity from the game
    (if destroy?
      [next-component-state [(ev/mk-event [:entity-remove entity-id]
                                          [:meta])]]
      next-component-state
      ;; [next-component-state [(ev/mk-event {:action :hit :direction :up}
      ;;                                     [:action entity-id])]]
      )))

;; If there are any collisions with events from entity-ids that start
;; with :attack- then handle damage (action event to trigger
;; animation, update hitpoints, emit event to remove entity)
(defn damage
  [entity-id component-state {:keys [inbox]}]
  (let [attacks (for [event inbox
                      collision (get-in event [:msg :collisions])
                      :let [collision-state (last collision)]
                      :when (valid-attack? entity-id collision-state)]
                  ;; Hashmap of :from-id :damage :type :position
                  (assoc (:attributes collision-state)
                         :position (take 2 collision)))]
    (if (seq attacks)
      (do ;; (println "HIT:" entity-id)
          (handle-damage entity-id component-state attacks))
      component-state)))
