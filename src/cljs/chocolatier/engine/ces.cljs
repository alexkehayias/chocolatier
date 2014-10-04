(ns chocolatier.engine.ces)

;; Gameloop:   recursive function that calls all systems in a scene
;;
;; Scenes:     collection of system functions called by the game loop
;;
;; Systems:    functions that operate on components and return
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
  "Add or update existing scene in the game state.

   system-ids are a collection of keywords referencing a system by
   their unique ID."
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
  (map #(% (:systems state)) system-ids))

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

(defn mk-entity
  "Adds entity with uid that has component-ids into state"
  [state uid component-ids]
  (assoc-in state [:entities uid] component-ids))

(defn get-component-fns
  [state component-id]
  (get-in state [:components component-id :fns]))

(defn get-component-state
  [state component-id entity-id]
  (or (get-in state [:components component-id :state entity-id])
      {}))

(defn mk-component-state
  "Returns a hashmap that can be merged into the state hashmap
   to store the state for the given component/entity id"
  [component-id entity-id val]
  {:components {component-id {:state {entity-id val}}}})

(defn mk-component-fn
  "Wraps component functions so they are called with the entity's 
   component state and the entity uid. Return value is wrapped in
   the component-state schema so it can be merged into global state.

   NOTE: The result of calling the component function must be mergeable 
   with the global state hashmap.

   Optional args:
   - args-fn: will be called with global state, component-id, entity-id 
     and the results will be applied to the component function f. This
     allows custom arguments to get access to any state in the game.
     NOTE: must return a sequence of arguments to be applied to f"
  [component-id f & [args-fn]]
  (fn [state entity-id]
    (if args-fn
      (apply f (args-fn state component-id entity-id))
      (mk-component-state
       component-id
       entity-id
       (f (get-component-state state component-id entity-id)
          entity-id)))))

(defn mk-component
  "Returns an updated state hashmap with the given component.
   Wraps each function in fns with mk-component-fn.

   - state: global state hashmap
   - uid: unique identifier for this component
   - fns: a vector of functions. Optionally these can be a pair of
     component fn and an args fn"
  [state uid fns]
  (let [wrapped-fns (for [f fns]
                      (if (satisfies? ISeqable f)
                        (apply (partial mk-component-fn uid) f)
                        (mk-component-fn uid f)))]
    ;; Add the component to state map and initialize component state
    (assoc state :components {uid {:fns wrapped-fns :state {}}})))

(defn mk-system-fn
  "Returns a function representing a system.

   Has two arrities:
   - [f] function f is called with state and wrapped to merge the return
     value of the function with state
   - [f component-id] function f is called with state, a collection of
     component functions, and a collection of entity ids that match the
     given component id. Results are merged with game state."
  ([f]
   (fn [state]
     (deep-merge state (f state))))  
  ([f component-id]
   (fn [state]
     (let [entities (entities-with-component (:entities state) component-id)
           component-fns (get-component-fns state component-id)]
       (deep-merge state (f state component-fns entities))))))

(defn mk-system
  "Add the system function to the state. Wraps the system function using
   mk-system-fn. Returns an update hashmap."
  [state uid f & [component-id]]
  (let [system-fn (if component-id
                    (mk-system-fn f component-id)
                    (mk-system-fn f))]
    (assoc-in state [:systems uid] system-fn)))
