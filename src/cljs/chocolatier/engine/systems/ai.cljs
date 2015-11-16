(ns chocolatier.engine.systems.ai
  "System for handling entity artificial intelligence"
  (:require [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]
            [clojure.core.reducers :as r]))


;; WARNING: This is a little bit fancy. We reduce over the output of
;; the component function, which uses the :format-fn option when making
;; the constructing the component fn to return updated state and
;; collection of events, to update the game state and accumulate
;; events along the way so they can be batch emitted instead of
;; individually for better performance. Don't follow this pattern
;; unless there are performance bottlenecks with sending messages

(defn accum-state-and-events
  "Returns a reducer function that accumulate the state and all events
   per entity-id. This allows us to batch the events which is faster.

   Parameterized by a function and context which should
   contain :player-state"
  [f context]
  (fn [[state-accum events-accum] entity-id]
    (let [moveable-state (ces/get-component-state state-accum :moveable entity-id)
          updated-context (assoc context :moveable-state moveable-state)
          [new-state events] (f state-accum entity-id updated-context)]
      [new-state (conj! events-accum [entity-id events])])))

(defn ai-system
  "Returns updated game state. Anyone participating chases :player 1"
  [state f entity-ids]
  (let [player (ces/get-component-state state :moveable :player1)
        context {:player-state player}
        [next-state events] (reduce (accum-state-and-events f context)
                                    [state (transient [])]
                                    entity-ids)]
    (ev/batch-emit-events next-state
                          [:move-change]
                          (into {} (persistent! events)))))
