(ns chocolatier.engine.ces)

;; Gameloop:   recursive function that calls all systems in a scene
;;
;; Scenes:     collection of systems used by the game loop
;;
;; Systems:    functions that operate on components and return
;;             the game state. They are represented as a nested
;;             hashmap i.e {:testable {:test fn1 :identity identity}}
;;
;; Entities:   unique IDs that have component implementation data
;;             Represented as a hashmap i.e {:player {:components []}}
;;
;; Components: hold state relating to a certain aspect.
;;             Queryable based on a entity uuid
;;
;; State:      a nested hashmap
;;             {:player
;;               {:testable {:x 1 :y 2}}
;;              :monster {:testable {:x 2 :y 3}}}
;;
;; Using one state hashmap should be performant enough even though it
;; create a new copy on every change to state in the game loop due to
;; structural sharing

(defn iter-fns
  "Pass an initial value through a collection of functions with the 
   result of the function called is passed as an arg to the next
   function."
  [init fn-coll]
  ((apply comp fn-coll) init))

(defn deep-merge
  "Recursively merges maps. If vals are not maps, the last value wins.
   via https://groups.google.com/forum/#!topic/clojure/UdFLYjLvNRs"
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn mk-component
  [component-id fields & [init-state]]
  (let [default-state (reduce into {} (for [f fields] {f nil}))
        init-state (or init-state default-state)]
    {component-id {:fields fields
                   :init-state init-state}}))

(defn add-component
  "Adds a component for the given entity-id and component"
  [state component]
  (assoc-in state [:components :_meta] component))

(defn mk-entity
  "Returns a hashmap representing the entities identity, components and systems.
   Validates 

   Example:
   (mk-entity :player {:moveable {:move f}})"
  [uuid components systems]
  ;; TODO validate that each component has all required component fns
  ;; implemented. Can we do this at compile time?

  [entity components systems])

(defn add-entity
  "Adds an entity to the game state hashmap or overwrites it if it
   already exists.

   Example:
   (let [[e c s] (mk-entity :p1 [] []
     (add-entity game-state e c s)"
  [state entity components systems]
  (deep-merge state {:entities entity
                     :components components
                     :systems systems}))

(defn add-system
  "Adds or overwrites a system for the given entity-id and component"
  [state entity-id component system f]
  (assoc-in state [:systems entity-id component system] f))

(defn remove-entity
  "Remove an entity and all of it's constituant components and systems"
  [])

(defn implements-component? [entity component]
  (boolean (some #{component} (:components entity))))

(defn filter-by-component
  "Takes a vector of id, hashmap pairs where the hashmap has a key
   for :components that the entitity implements.

   Returns a vector of ids that implement component."
  [entities component]
  (map first
       (filter #(implements-component? (second %) component) entities)))

(defn get-system-fns
  "Returns a vector of functions for the component."
  [systems ids component]
  (map #(get-in systems [% component]) ids))

;; This only operates on entities, can a system work on something
;; other than entities? What about game meta data?
(defn exec-system
  "Execute the component by calling each component function in order."
  [state component fn-keys]
  (let [;; Get all entities that implement this component
        ids (filter-by-component (seq (:entities state)) component)
        ;; Get the system for the matching component
        component-fns (get-system-fns (:systems state) ids component)
        ;; Make a sequence of all the functions to call in order as
        ;; specified by fn-keys
        fns (reduce into [] (for [k fn-keys] (map k component-fns)))] 
    (iter-fns state fns)))

(defn mk-system-spec
  "Convenience wrapper so you don't have to specify vectors of vectors.

   Example:
   (mk-system-spec [:my-sys [:f1 :f2]]
                   [:my-other-sys [:f3 :f4]])"
  [& specs] specs)
