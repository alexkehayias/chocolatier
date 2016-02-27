(ns chocolatier.engine.events)

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

(def subscription-path
  [:state :events :subscriptions])

(defn get-events
  "Returns a collection of events or nil"
  [state selectors]
  (let [events (get-in state (into queue-path selectors))]
    ;; Selectors represent a nested path of keys so if the selector is
    ;; another map, throw an error
    (assert (not (map? events)) (str "Invalid event selector: " selectors))
    events))

(defn get-subscribed-events
  "Returns a collection of events that matches the collection of
   selectors or nil if selectors-col is empty."
  [state selectors-col]
  (reduce #(into %1 (get-events state %2)) [] selectors-col))

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
    (assert (every? keyword? selectors))
    (update-in state path conj event)))

(defn emit-events
  "Emits a collection of events at the same time. Returns update game state."
  [state events]
  (if (seq events)
    (reduce #(emit-event %1 (:msg %2) (:selectors %2)) state events)
    state))

(defn batch-emit-events
  "Batch add events with the same selectors. Events should be a hashmap of id,
   collection of valid events (see mk-event). Will merge existing events map with
   events-map overwriting existing keys"
  [state selectors events-map]
  (let [path (into queue-path selectors)]
    (update-in state path merge events-map)))

(defn clear-events-queue
  "Resets event queue to an empty vector. Returns updates state."
  [state]
  (assoc-in state queue-path {}))