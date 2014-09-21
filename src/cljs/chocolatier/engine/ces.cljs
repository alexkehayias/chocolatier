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
  "Wraps function f with parameters for the component state for the
   corresponding entity. Return results of f are formatted so it can be
   merged into the game state."
  [component-id f]
  (fn [state entity-id]
    (mk-component-state
     component-id
     entity-id
     (f (get-component-state state component-id entity-id)
        entity-id))))

(defn mk-component
  "Returns an updated state hashmap with the given component.
   Wraps each function in fns with mk-component-fn.

   The return value of any component function should be the updated
   component state which will be merged into the overall state"
  [state uid fns]
  (let [wrapped-fns (for [f fns] (mk-component-fn uid f))]
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
