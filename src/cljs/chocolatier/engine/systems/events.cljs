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


(defn init-events-system
  "Adds an :events entry to the state hashmap."
  [state]
  (assoc-in state [:state :events] {:queue [] :subscriptions {}}))

(defn subscribe
  "Subscribe to the given event. Optionally pass in a function that takes
   an event and returns true or false to keep or discard it. Multiple subscribe
   calls with the same event-id component-id entity-id will not duplicate
   subscriptions."
  [state event-id component-id entity-id & [f]]
  ;; If no function is supplied, the events parsing function is an identity
  (assoc-in state [:state :events :subscriptions event-id component-id entity-id]
            (or f (fn [& args] true))))

(defn msg->subscribers
  "Fans out messages to subscribers. 
   Returns a lazy seq of events with subscriber information."
  [queue subscriptions]
  (for [[event-id from msg] queue
        [component-id entity-subs] (-> subscriptions event-id)
        [entity-id add-to-inbox?] entity-subs]
    (if (add-to-inbox? event-id from msg)
      [event-id from msg component-id entity-id])))

(defn to-inbox
  "Adds a message to the inbox of all subscribers of the event.
   Returns updated game state."
  [state subscriber-event]
  (let [[event-id from msg component-id entity-id] subscriber-event]
    (update-in state [:state :inbox component-id entity-id]
               conj {:event-id event-id :from from :msg msg})))

(defn clear-inbox
  "Remove all component messages for the give entity IDs"
  [state entity-ids component-id]
  (if (seq entity-ids)
    (assoc-in state [:state :inbox component-id]
              (into {} (map #(vector % []) entity-ids)))
    state))

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
    (assert (map? msg) "msg is a hash-map")))

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
    (do (doseq [e events] (valid-event? e))
        (update-in state [:state :events :queue] concat events)) 
    state))

(defn clear-events-queue
  "Resets event queue to an empty vector. Returns updates state."
  [state]
  (assoc-in state [:state :events :queue] []))

(defn event-system
  "Send a message to all mailboxes that are subscribed to any events,
   clear out events queue. Returns update game state."
  [state]
  (let [{:keys [queue subscriptions]} (-> state :state :events)
        events (msg->subscribers queue subscriptions)]
    (clear-events-queue (reduce to-inbox state events))))
