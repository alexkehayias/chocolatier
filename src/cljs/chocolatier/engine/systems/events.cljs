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


(defn mk-events-system
  "Adds an :events entry to the state hashmap."
  [state]
  (assoc-in state [:state :events] {:queue [] :subscriptions {}}))

(defn subscribe
  "Subscribe to the given event. Optionally pass in a functiyon that takes
   a queue of events and returns a vector."
  [state event-id entity-id component-id & [f]]
  ;; If no function is supplied, the events parsing function is an identity
  (assoc-in state [:state :events :subscriptions event-id entity-id component-id]
            (or f (fn [& args] true))))

(defn msg->subscribers
  "Fans out messages to subscribers"
  [queue subscriptions]
  (for [[event-id from msg] queue
        [entity-id components] (-> subscriptions event-id)
        [component-id add-to-inbox?] components]
    (if (add-to-inbox? event-id from msg)
      [event-id from msg entity-id component-id])))

(defn to-inbox
  [state event]
  (let [[event-id from msg entity-id component-id] event]
    (update-in state [:state :inbox entity-id component-id]
               conj {:event-id event-id :from from :msg msg})))

(defn valid-event?
  "Asserts the validity of an event. A properly formed event has the
   following items in the event tuple:
   - event-id is a keyword
   - from is the sender of the message as a keyword
   - msg is a hashmap"
  [event]
  (let [[event-id from msg] event]
    (assert (keyword? event-id) "event-id is a keyword")
    (assert (keyword? from) "from is a keyword")
    (assert (map? from) "msg is a hash-map")))

(defn emit-event
  "Enqueues an event onto the queue"
  [state event-id from msg]
  (assert valid-event? [event-id from msg])
  (update-in state [:state :events :queue] conj [event-id from msg]))

(defn emit-events
  "Emits a collection of events at the same time. If events is nil then
   return the state unchanged."
  [state events]
  (if events
    (doseq [e events] (valid-event? e))
    (update-in state [:state :events :queue] concat events)
    state))

(defn event-system
  "Send a message to all mailboxes that are subscribed to any events"
  [state]
  (let [{:keys [queue subscriptions]} (-> state :state :events)
        events (msg->subscribers queue subscriptions)]
    (reduce to-inbox state events)))
