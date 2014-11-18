(ns chocolatier.engine.ces
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.systems.events :as ev]))

;; Gameloop:   recursive function that calls all systems in a scene
;;
;; Scenes:     collection of system functions called by the game loop
;;
;; Systems:    functions that operates on a components and returns
;;             updated game state.
;;
;; Entities:   unique IDs that have a list of components to
;;             participate in
;;
;; Components: hold state and lists of functions relating to a certain aspect.
;;
;; State:      stores state for components, entities, and systems
;;
;; Using one state hashmap should be performant enough even though it
;; creates a new copy on every change to state in the game loop due to
;; structural sharing


(defn mk-scene
  "Add or update existing scene in the game state. A scene is a
   collection of systems. system-ids are a collection of keywords referencing
   a system by their unique ID."
  [state uid system-ids]
  (assoc-in state [:scenes uid] system-ids))

(defn iter-fns
  "Pass an initial value through a collection of functions with the 
   result of the function called is passed as an arg to the next
   function."
  [init fn-coll]
  ((apply comp fn-coll) init))

(defn get-system-fns
  "Return system functions with an id that matches system-ids in order."
  [state system-ids]
  (map #(or (% (:systems state))
            (throw (js/Error. (str "System " % " not found") % )))
       system-ids))

(defn deep-merge
  "Recursively merges maps. If vals are not maps, the last value wins.
   via https://groups.google.com/forum/#!topic/clojure/UdFLYjLvNRs"
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn entities-with-component
  "Takes a hashmap and returns all keys whose values contain component-id"
  [entities component-id]
  (map first
       (filter #(boolean (some #{component-id} (second %))) entities)))

(defn entities-with-multi-component
  "Takes a hashmap and returns all keys whose values has all component-ids"
  [entities component-ids]
  (map first
       (filter #(boolean (some #{component-ids} (second %))) entities)))

(defn mk-entity
  "Adds entity with uid that has component-ids into state"
  [state uid component-ids]
  (assoc-in state [:entities uid] component-ids))

(defn get-event-inbox
  "Returns a collections of event hashmaps representing the inbox of the given
   component-id/entity-id. Returns an empty vector there are no events."
  [state component-id entity-id]
  (or (get-in state [:state :inbox component-id entity-id]) []))

(defn get-component-fns
  [state component-id]
  (or (get-in state [:components component-id :fns])
      (throw (js/Error. (str "No component functions found for " component-id
                             " in" (-> state :components))))))

(defn get-component-state
  "Returns a hashmap of state associated with the component for the given
   entity. NOTE: As a convenience, if state is not found it returns an empty
   hashmap."
  [state component-id entity-id]
  (or (get-in state [:state component-id entity-id]) {}))

(defn mk-component-state
  "Returns an updated hashmap with component state for the given entity"
  [state component-id entity-id val]
  (assoc-in state [:state component-id entity-id] val))

(defn update-component-state-and-events
  "Update a components state. If there are events then also add those to
   game state."
  [state component-id entity-id val & [events]]
  (-> state
      (mk-component-state component-id entity-id val)
      (ev/emit-events events)))

(defn all-not-nil?
  "Returns true if any of the items in coll are nil"
  [coll]
  (empty? (filter nil? coll)))

(defn mk-component-fn
  "Returns a function that is called with game state and the entity id.

   Wraps a component function so it is called with the entity id, 
   component state, and event inbox. Return value of f is wrapped in the
   format-fn or update-component-state-and-events which returns an updated 
   game state.

   The component function can return 1 result or 2. If 1 result then the
   output is treated as the component state. If it is 2 then the second
   argument is events that should be emitted. If format-fn is specified
   then you can implement whatever handling of results you want.

   NOTE: The result of calling the component function must be an updated 
   game state hashmap.

   Optional args:
   - options: Hashmap of options for a component function including:
     - args-fn: will be called with global state, component-id, entity-id 
       and the results will be applied to the component function f. This
       allows custom arguments to get access to any state in the game.
       NOTE: must return a collection of arguments to be applied to f
     - format-fn: called with component-id, entity-id and the result of f,
       must return a mergeable hashmap"
  [component-id f & [{:keys [args-fn format-fn]} options]]
  (fn [state entity-id]
    (let [args (if args-fn
                 (args-fn state component-id entity-id)
                 ;; Default args to the component function
                 [entity-id
                  (get-component-state state component-id entity-id)
                  (get-event-inbox state component-id entity-id)])
          result (apply f args)
          output-fn (partial (or format-fn update-component-state-and-events)
                             state component-id entity-id)]
      ;; Handle if the result is going to include events or not
      (if (vector? result)
        (do
          ;; Make sure the results are not more than 2 items and not
          ;; an empty vector
          (assert (and (<= (count result) 2) (not (empty? result))))
          ;; Make sure that the items in the list are not nil
          (assert (all-not-nil? result))
          (apply output-fn result))
        (do
          ;; Make sure the result is a hashmap (updated state)
          (assert (map? result))
          (output-fn result))))))

(defn mk-component
  "Returns an updated state hashmap with the given component.
   Wraps each function in fns with mk-component-fn.

   - state: global state hashmap
   - uid: unique identifier for this component
   - fns: a vector of functions. Optionally these can be a pair of
     component fn and an args fn"
  [state uid fn-specs]
  (let [wrapped-fns (for [spec fn-specs]
                      (if (satisfies? ISeqable spec)
                        (do (log/debug "mk-component: found options" spec)
                            (apply (partial mk-component-fn uid) spec))
                        (mk-component-fn uid spec)))]
    ;; Add the component to state map
    ;; TODO do we need a :fns keyword? there's no other data stored here
    (assoc-in state [:components uid] {:fns wrapped-fns})))

(defn mk-system-fn
  "Returns a function representing a system.

   Has two arrities:
   - [f] function f is called with state and returns updated game state
   - [f component-id] function f is called with state, a collection of
     component functions, and a collection of entity ids that match the
     given component id. Return result is updated game state and inbox
     messages are removed."
  ([f]
   (fn [state] (f state)))  
  ([f component-id]
   (fn [state]
     (let [entities (entities-with-component (:entities state) component-id)
           component-fns (get-component-fns state component-id)
           updated-state (f state component-fns entities)]
       (ev/clear-inbox updated-state entities component-id))))
  ([f component-id & more-component-ids]
   (fn [state]
     (let [ids (conj component-ids more-component-id)
           entities (entities-with-multi-components (:entities state) ids)
           component-fns (get-component-fns state component-id)]
       ;; Update game state
       (-> (f state component-fns entities)
           ;; Clear out inbox from all the system's components
           ;; (implicit acknowledgement of the messages)
           (ev/clear-inbox entities component-id)
           ;; Emit all the messages to subscribers
           (ev/fan-out-messages)
           ;; Clear events queue
           (ev/clear-events-queue))))))

(defn mk-system
  "Add the system function to the state. Wraps the system function using
   mk-system-fn. Returns an update hashmap."
  [state uid f & [component-id]]
  (log/debug "mk-system:" uid "component-id:" (or component-id "nil"))
  (let [system-fn (if component-id
                    (mk-system-fn f component-id)
                    (mk-system-fn f))]
    (assoc-in state [:systems uid] system-fn)))
