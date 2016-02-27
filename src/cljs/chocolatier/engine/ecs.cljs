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
   collection of systems. system-ids are a collection of keywords referencing
   a system by their unique ID."
  [state uid system-ids]
  (assoc-in state [:scenes uid] system-ids))

(def scene-id-path
  [:game :scene-id])

(defn mk-current-scene
  "Sets the current scene of the game"
  [state scene-id]
  (assoc-in state scene-id-path scene-id))

(defn mk-renderer
  [state renderer stage]
  (assoc-in state [:game :rendering-engine] {:renderer renderer :stage stage}))

(defn get-system-fns
  "Return system functions with an id that matches system-ids in order.
   If a key is not found it will not be returned."
  [state system-ids]
  (let [systems (:systems state)]
    (mapv systems system-ids)))

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
    (reduce (fn [accum [entity-id entity-component-set]]
              (if (subset? component-set entity-component-set)
                (conj accum entity-id)
                accum))
            #{}
            (:entities state))))

(defn get-component-fn
  [state component-id]
  (or (get-in state [:components component-id :fn])
      (throw (js/Error. (str "No component function found for " component-id)))))

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
  [state component-id entity-id val]
  (assoc-in state [:state component-id entity-id] val))

(defn update-component-state-and-events
  "Update a components state. If there are events then also add those to
  game state."
  ([state component-id entity-id val]
   (mk-component-state state component-id entity-id val))
  ([state component-id entity-id val events]
   (-> state
       (mk-component-state component-id entity-id val)
       (ev/emit-events events))))

(defn- component-fn-body
  "Use mk-component-fn to construct a component function body"
  [f state component-id entity-id
   {:keys [args-fn format-fn subscriptions component-states]}
   sys-opts]
  (let [opts (cond-> (if args-fn
                       (args-fn state component-id entity-id)
                       {})
               component-states (assoc :component-states
                                       (map #(get-component-state state % entity-id)
                                            component-states))
               subscriptions (assoc :inbox (ev/get-subscribed-events
                                            state
                                            ;; Implicitely add the
                                            ;; entity ID to the end of
                                            ;; the selectors, this
                                            ;; ensures messages are
                                            ;; per entity
                                            (map #(vector % entity-id) subscriptions)))
               sys-opts (merge sys-opts))
        component-state (get-component-state state component-id entity-id)
        ;; Pass args and system argument to the component function
        result (f entity-id component-state opts)
        output-fn (or format-fn update-component-state-and-events)]
    ;; Handle if the result is going to include events or not
    (if (vector? result)
      (let [[component-state events :as r] result]
        ;; Make sure the results are 2 items and not nil
        (assert (not-any? nil? r)
                (str "Component fn did not return valid vector: " result))
        (output-fn state component-id entity-id component-state events))
      (do
        ;; Make sure the result is a hashmap (updated state)
        (assert (map? result)
                (str "Component fn did not return a valid hashmap: " result))
        (output-fn state component-id entity-id result)))))

(defn mk-component-fn
  "Returns a function that is called with game state and the entity id.

   Wraps a component function so it is called with the entity id,
   component state, and event inbox. Return value of f is wrapped in the
   format-fn or update-component-state-and-events which returns an updated
   game state.

   To have the component function take an argument passed in by the system
   function, the component function must take it as the last argument even
   if a :args-fn is specified. This is useful for optimizations where each
   entity would otherwise perform the same operation multiple times.

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
       must return a mergeable hashmap
     - cleanup-fn: called with global state and entity-id and must return
       updated game state. Useful for cleaning up side effects like sprites
     - subscriptions: a collection of vectors of selectors of events to be
       included in the inbox argument of the component-fn. Implicitely adds
       the entity-id as the last selector so that component subscriptions
       are always per entity."
  [component-id component-fn & [opts]]
  ;; Using multiple arrities as an optimization for dynamic
  ;; dispatching of optional args in the fn signature
  (fn
    ([state entity-id]
     (component-fn-body component-fn
                        state
                        component-id
                        entity-id
                        opts
                        nil))
    ([state entity-id sys-opts]
     (component-fn-body component-fn
                        state
                        component-id
                        entity-id
                        opts
                        sys-opts))))

(defn mk-component
  "Returns an updated state hashmap with the given component.
   Wraps function with mk-component-fn.

   - state: global state hashmap
   - uid: unique identifier for this component
   - fn-spec: [f {<opts>}]"
  [state uid fn-spec]
  (let [opts? (seqable? fn-spec)
        wrapped-fn (if opts?
                     (do (log/debug "mk-component: found options for"
                                    uid (keys (second fn-spec)))
                         (apply mk-component-fn uid fn-spec))
                     (mk-component-fn uid fn-spec))
        cleanup-fn (when opts? (:cleanup-fn (second fn-spec)))]
    (update-in state [:components uid] assoc
               :fn wrapped-fn
               :cleanup-fn cleanup-fn)))

(defn mk-system-fn
  "Returns a function representing a system.

   Has three arrities:
   - [f] function f is called with state and returns updated game state
   - [f component-id] function f is called with state, a collection of
     component functions, and a collection of entity ids that match the
     given component id. Return result is updated game state and inbox
     messages are removed.
   - [f component-id & more-ids] function f is called with state, a collection
     of component functions for component-id, and a collection of entity ids that
     match ALL given component ids. Return result is updated game state and inbox
     messages are removed."
  ([f] f)
  ([f component-id]
   (fn [state]
     (let [entities (entities-with-component state component-id)
           component-fn (get-component-fn state component-id)]
       (f state component-fn entities))))
  ([f component-id & more-component-ids]
   (fn [state]
     (let [ids (conj more-component-ids component-id)
           entities (entities-with-multi-components state ids)
           component-fn (get-component-fn state component-id)]
       (f state component-fn entities)))))

(defn mk-system
  "Add the system function to the state. Wraps the system function using
   mk-system-fn. Returns an update hashmap."
  [state uid f & [component-id]]
  (log/debug "mk-system:" uid (when component-id
                                (str "component-id: " component-id)))
  (let [system-fn (if component-id
                    (mk-system-fn f component-id)
                    (mk-system-fn f))]
    (assoc-in state [:systems uid] system-fn)))

(defn component-state-from-spec
  "Returns a function that returns an updated state with component state
   generated for the given entity-id. If no initial component state is given,
   it will default to an empty hashmap."
  [entity-id]
  (fn [state spec]
    (if (vector? spec)
      (let [[component-id component-state] spec]
        (-> state
            (update-in [:entities entity-id]
                       #(conj (or % #{}) component-id))
            (update-in [:components component-id :entities]
                       #(conj (or % #{}) entity-id))
            (mk-component-state component-id entity-id component-state)))
      (-> state
          (update-in [:entities entity-id]
                     #(conj (or % #{}) spec))
          (update-in [:components spec :entities]
                     #(conj (or % #{}) entity-id))
          (mk-component-state spec entity-id {})))))

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
  [state uid components]
  (reduce (component-state-from-spec uid) state components))

(defn rm-entity-from-component-index
  [state entity-id components]
  (reduce (fn [state component-id]
            (update-in state [:components component-id :entities]
                       #(set (remove #{entity-id} %))))
          state
          components))

(defn rm-entity
  "Remove the specified entity and return updated game state"
  [state uid]
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