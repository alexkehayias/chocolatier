(ns chocolatier.engine.components.health
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))

(defn handle-player-damage
  [inbox]
  ;; TODO get from
  )

(defn handle-enemy-damage
  [inbox]
  ;; TODO get from
  )

(defn calculate-damage
  [entity-id inbox]
  ;; Purposely avoiding multimethods here because they are slow
  (condp keyword-identical? entity-id
    :player1 (handle-player-damage inbox)
    (handle-enemy-damage inbox)))

(defn health
  "If there is a collision event from :player1 -> an enemy or an enemy
  -> :player1, handle damage or destruction"
  [entity-id component-state {:keys [inbox]}]
  ;; If there is damage, emit a text
  (if (seq inbox)
    (calculate-damage entity-id inbox)
    component-state))
