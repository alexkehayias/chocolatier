(ns chocolatier.engine.systems.events
  (:require [chocolatier.utils.logging :refer [debug]]
            [chocolatier.engine.utils.watchers :refer [seq-watcher]]))

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
  "Subscribe to the given event. Optionally pass in a function that takes
   an event and returns true or false to keep or discard it. Multiple subscribe
   calls with the same event-id component-id entity-id will not duplicate
   subscriptions."
  [state entity-id event-id & selectors]
  ;; If no function is supplied, the events parsing function is an identity
  (update-in state [:state :events :subscriptions entity-id]
             conj (if (empty? selectors)
                    event-id
                    (conj selectors event-id))))

(defn get-subscribed-events
  "Returns a lazy seq of events for entity-id based on their subscriptions"
  [state entity-id]
  (let [subscriptions (get-in state [:state :events :subscriptions entity-id])
        events (get-in state [:state :events :queue])]
    (mapcat #(get-in events (if (seqable? %) % [%])) subscriptions)))

(defn valid-event?
  "Asserts the validity of an event. A properly formed event has the
   following items in the event tuple:
   - event-id is a keyword
   - from is the sender of the message as a keyword
   - msg is a hashmap"
  [{:keys [event-id msg] :as event}]
  (assert (map? msg) "msg is not a hash-map")
  (doseq [s event-id] (assert (keyword? s) "selector is not a keyword")))

(defn mk-event [msg & selectors]
  {:selectors selectors :msg msg})

(defn emit-event
  "Enqueues an event onto the queue"
  [state msg & selectors]
  (let [event (apply mk-event msg selectors)]
    (valid-event? event)
    (update-in state (concat [:state :events :queue] selectors) conj event)))

(defn emit-events
  "Emits a collection of events at the same time. Returns update game state."
  [state events]
  (reduce #(apply emit-event %1 (:msg %2) (:selectors %2)) state events))

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
