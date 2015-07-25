(ns chocolatier.engine.systems.events
  (:require [chocolatier.utils.logging :refer [debug]]))

;; Anyone can send a message to the event bus
;; The event system distributes the events to any subscribers into
;; their inbox component state
;; Events are tuples of event keyword, the sender, and message hm
;; Messages should only be used for asynchronous communication
;; Avoid sending messages based recieving a message to prevent
;; circular messages happening

;; This needs to be globally accessible to all systems and component
;; functions

;; How do we do this without a singleton?
;; Update the component function to get the component state and it's
;; mailbox. Mailboxes are per entity/component.
;; Add a function to add a subscription for a given
;; event/entity/component id


(defn subscribe
  "Subscribe to the given event.

   Multiple subscribe calls with the same event-id component-id entity-id
   are idempotent.

   Example:
   Subscribe to the move change events of the entity :player1
   (subscribe {} :player1 :move-change :player1)"
  [state entity-id & selectors]
  (update-in state [:state :events :subscriptions entity-id] conj selectors))

(defn multi-subscribe
  "Subscribe the entity to multiple events at once.

   Example:
   Subscribe to the move change and collision events of the entity :player1
   (multi-subscribe {} :player1 [[:move-change :player1]
                                 [:collision :player1]])"
  [state entity-id selector-coll]
  (if-let [selectors (first selector-coll)]
    (recur (apply (partial subscribe state entity-id) selectors)
           entity-id
           (rest selector-coll))
    state))

(defn get-events
  "Returns a lazy sequence of events matching the selectors.
   Selectors are scoped from general to more specific.

   For example:
   - [:s1 :s2 :s3] matches all events with all 3 selectors
   - [:s1] matches all results in the :s2 and :s3 key"
  [state selectors]
  (when (seq selectors)
    (let [result (get-in state (into [:state :events :queue] selectors))]
      (if (map? result)
        (mapcat #(get-events state (conj selectors %)) (keys result))
        result))))

(defn get-subscribed-events
  "Returns a lazy seq of events for entity-id based on their subscriptions"
  [state entity-id]
  (mapcat #(get-events state %)
          (get-in state [:state :events :subscriptions entity-id])))

(defn valid-event?
  "Asserts the validity of an event. A properly formed event has the
   following items in the event tuple:
   - event-id is a keyword
   - from is the sender of the message as a keyword
   - msg is a hashmap"
  [{:keys [selectors msg] :as event}]
  (assert (map? msg) "msg is not a hash-map")
  (doseq [s selectors] (assert (keyword? s) "selector is not a keyword")))

(defn mk-event
  "Takes message and selectors and formats them for the event representation.
   The first selector, by convention, is called the event-d. Returns a hashmap."
  [msg & selectors]
  {:event-id (first selectors) :selectors selectors :msg msg})

(defn emit-event
  "Enqueues an event onto the queue"
  [state msg & selectors]
  (let [event (apply mk-event msg selectors)]
    (valid-event? event)
    (update-in state (into [:state :events :queue] selectors) conj event)))

(defn emit-events
  "Emits a collection of events at the same time. Returns update game state."
  [state events]
  (if (seq events)
    (reduce #(apply emit-event %1 (:msg %2) (:selectors %2)) state events)
    state))

(defn clear-events-queue
  "Resets event queue to an empty vector. Returns updates state."
  [state]
  (assoc-in state [:state :events :queue] {}))

(defn init-events-system
  "Adds an :events entry to the state hashmap."
  [state]
  (assoc-in state [:state :events] {:queue {} :subscriptions {}}))

;; TODO manage subscriptions here too
(defn event-system
  "Clear out events queue. Returns update game state."
  [state]
  (clear-events-queue state))
