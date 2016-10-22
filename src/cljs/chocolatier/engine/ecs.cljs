(ns chocolatier.engine.ecs
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.events :as ev]
            [clojure.set :refer [subset?]]))

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
   collection of systems. systems are a collection of keywords referencing
   a system by their unique ID."
  [state {:keys [uid systems]}]
  (assoc-in state [:scenes uid] systems))

(def scene-id-path
  [:game :scene-id])

(defn mk-current-scene
  "Sets the current scene of the game"
  [state {scene-id :uid}]
  (assoc-in state scene-id-path scene-id))

(defn mk-renderer
  [state {:keys [renderer stage]}]
  (assoc-in state [:game :rendering-engine] {:renderer renderer :stage stage}))

(defn get-system-fns
  "Return system functions with an id that matches system-ids in order.
   If a key is not found it will not be returned."
  [state system-ids]
  (let [systems (:systems state)]
    (doall (map systems system-ids))))

(defn entities-with-component
  "Returns a set of all entity IDs that have the specified component-id"
  [state component-id]
  (get-in state [:components component-id :entities] #{}))

(defn entities-with-multi-components
  "Returns a set of all entity IDs that have all the specified component-ids."
  [state component-ids]
  (let [component-set (set component-ids)]
    ;; Iterate through all the entities and only accumulate the ones
    ;; that have all the required component IDs
    (loop [entities (:entities state)
           accum (transient #{})]
      (let [entity (first entities)]
        (if entity
          (let [[entity-id entity-component-set] entity]
            (if ^boolean (subset? component-set entity-component-set)
                (recur (rest entities) (conj! accum entity-id))
                (recur (rest entities) accum)))
          (persistent! accum))))))

(defn get-component
  "Returns the component meta as a hashmap of
   :fn :subscriptions :select-components"
  [state component-id]
  (get-in state [:components component-id]))

(defn get-component-state
  "Returns a hashmap of state associated with the component for the given
   entity. NOTE: As a convenience, if state is not found it returns an empty
   hashmap."
  [state component-id entity-id]
  (get-in state [:state component-id entity-id] {}))

(defn get-all-component-state
  [state component-id]
  (get-in state [:state component-id]))

(defn mk-component-state
  "Returns an updated hashmap with component state for the given entity"
  [state component-id entity-id init-component-state]
  (assoc-in state [:state component-id entity-id] init-component-state))

(defn mk-component
  "Returns an updated state hashmap with the given component.

   Args:
   - state: global state hashmap
   - uid: unique identifier for this component
   - fn-spec: [f {<opts>}] or f

   Supported component options:
   - subscriptions: a collection of selectors of messages to receive.
     This will be included as a sequence in the context passed to the
     component fn in the :inbox key
   - select-components: a collection of component IDs of additional state
     to select which will be available in the :select-components key of
     the context passed to the component fn
   - cleanup-fn: called when removing the entity and all it's components.
     This should perform any other cleanup or side effects needed to remove
     the component and all of it's state completely"
  [state uid {:keys [fn cleanup-fn subscriptions select-components]}]
  (assert fn "Invalid component, missing :fn key.")
  (update-in state [:components uid]
             merge {:fn fn
                    :subscriptions subscriptions
                    :select-components select-components
                    :cleanup-fn cleanup-fn}))

(defn concat-keywords [k1 k2]
  (keyword (str (name k1) "-" (name k2))))

(defn get-component-context
  "Returns a hashmap of context for use with a component fn.
   Args:
   - state: The game state
   - queue: The events queue
   - entity-id: The unique ID of the entity
   - component: A hashmap representing the component meta data"
  [state queue component entity-id]
  (let [{:keys [subscriptions select-components]} component
        messages (ev/get-subscribed-events queue entity-id subscriptions)]
    ;; Add in any selected components
    (loop [components select-components
           context (transient {:inbox messages})]
      (let [component (first components)]
        (if component
          ;; If it was a vector then the first arg is the
          ;; component-id the second is a specific entity-id
          (let [next-context (if (vector? component)
                               (let [[component entity-id] component
                                     key (concat-keywords component entity-id)]
                                 (assoc! context key
                                         (get-component-state state component entity-id)))
                               (assoc! context component
                                       (get-component-state state component entity-id)))]
            (recur (rest components) next-context))
          (persistent! context))))))

(defn system-next-state-and-events
  [state component-id]
  (let [entity-ids (entities-with-component state component-id)
        component-states (get-all-component-state state component-id)
        component (get-component state component-id)
        component-fn (:fn component)
        queue (get-in state ev/queue-path)]
    (loop [entities entity-ids
           state-accum (transient {})
           event-accum (array)]
      (let [entity-id (first entities)]
        (if entity-id
          (let [component-state (get component-states entity-id)
                context (get-component-context state queue component entity-id)
                next-comp-state (component-fn entity-id component-state context)
                ;; If the component function returns a vector then there
                ;; are events to accumulate
                next-state (if (vector? next-comp-state)
                             (let [[next-comp-state events] next-comp-state]
                               (doseq [e events]
                                 (.push event-accum e))
                               (assoc! state-accum entity-id next-comp-state))
                             (assoc! state-accum entity-id next-comp-state))]
            (recur (rest entities) next-state event-accum))
          [(assoc-in state [:state component-id] (persistent! state-accum))
           event-accum])))))

(defn mk-system-fn
  "Returns a function representing a system that takes a single argument for
   game state."
  [component-id]
  (fn [state]
    (let [[next-state events] (system-next-state-and-events state component-id)]
      (ev/emit-events next-state events))))

(defn mk-system
  "Add the system function to the state."
  [state {:keys [component uid fn]}]
  (if component
    (let [component-id (:uid component)]
      (log/debug "mk-system:" uid "that operates on component-id:" component-id)
      (-> state
          (assoc-in [:systems uid] (mk-system-fn component-id))
          (mk-component component-id component)))
    (do
      (log/debug "mk-system:" uid)
      (assert fn "Invalid system spec, missing :fn")
      (assoc-in state [:systems uid] fn))))

(defn component-state-from-spec
  "Returns a function that returns an updated state with component state
   generated for the given entity-id. If no initial component state is given,
   it will default to an empty hashmap."
  [entity-id]
  (fn [state {component-id :uid component-state :state}]
    (-> state
        (update-in [:entities entity-id]
                   #(conj (or % #{}) component-id))
        (update-in [:components component-id :entities]
                   #(conj (or % #{}) entity-id))
        (mk-component-state component-id entity-id component-state))))

(defn mk-entity
  "Adds entity with uid that has component-ids into state. Optionally pass
   in init state and it will be merged in

   Component specs:
   A collection of component IDs and/or 2 item vectors of the component ID
   and hashmap of component-state.
   e.g [[:moveable {:x 0 :y 0}] :controllable]

   Example:
   Create an entity with id :player1 with components
   (mk-entity {}
              :player1
              [:controllable
               [:collidable {:hit-radius 10}]
               [:moveable {:x 0 :y 0}]])"
  [state {:keys [uid components]}]
  (reduce (component-state-from-spec uid) state components))

(defn rm-entity-from-component-index
  "Remove the entity-id from the component entity index. Returns updated state."
  [state entity-id components]
  (reduce (fn [state component-id]
            (update-in state [:components component-id :entities]
                       #(set (remove #{entity-id} %))))
          state
          components))

(defn rm-entity
  "Remove the specified entity and return updated game state"
  [state {:keys [uid]}]
  (let [components (get-in state [:entities uid])]
    (as-> state $
      ;; Call cleanup function for the component if it's there
      (reduce #(if-let [f (get-in %1 [:components %2 :cleanup-fn])]
                 (f %1 uid)
                 %1)
              $
              components)
      (reduce #(update-in %1 [:state %2] dissoc uid) $ components)
      ;; Remove the entity
      (update-in $ [:entities] dissoc uid)
      ;; Remove the entity from component index
      (rm-entity-from-component-index $ uid components))))
