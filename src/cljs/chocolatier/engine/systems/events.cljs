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


(def queue-path
  [:state :events :queue])

(defn get-events
  "Returns a collecition of events or nil"
  [state selectors]
  (get-in state (concat queue-path selectors)))

(defn get-subscribed-events
  "Returns a collection of events that matches the collection of subscriptions
   or nil if the subscriptions are empty"
  [state subscriptions]
  (loop [s subscriptions
         accum (transient [])]
    (if-let [[selectors & more] s]
      (do
        (doseq [e (get-events state selectors)]
          (conj! accum e))
        (recur more accum))
      (persistent! accum))))

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
  [msg selectors]
  {:event-id (selectors 0) :selectors selectors :msg msg})

(defn emit-event
  "Enqueues an event onto the queue"
  [state msg selectors]
  (let [event (mk-event msg selectors)
        path (into queue-path selectors)]
    ;; TODO only include this if we are in dev mode
    ;; (valid-event? event)
    (update-in state path conj event)))

(defn emit-events
  "Emits a collection of events at the same time. Returns update game state."
  [state events]
  (if (seq events)
    (reduce #(emit-event %1 (:msg %2) (:selectors %2)) state events)
    state))

(defn clear-events-queue
  "Resets event queue to an empty vector. Returns updates state."
  [state]
  (assoc-in state queue-path {}))

(defn init-events-system
  "Adds an :events entry to the state hashmap."
  [state]
  (assoc-in state [:state :events] {:queue {} :subscriptions {}}))

;; TODO manage subscriptions here too
(defn event-system
  "Clear out events queue. Returns update game state."
  [state]
  (clear-events-queue state))
