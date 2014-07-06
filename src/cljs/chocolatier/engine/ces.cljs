(ns chocolatier.engine.ces)

;; Gameloop:   recursive function that calls all systems in a scene
;;
;; Scenes:     collection of systems used by the game loop
;;
;; Systems:    functions that operate on components and return
;;             the game state
;;
;; Entities:   unique IDs that have component implementation data
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

(defn mk-component
  [name fields fns]
  {:fields fields :fns fns})

(defn mk-entity
  "
  Example
  (mk-entity \"Player\" {:moveable {:move #(identity %)}} )
   "
  [uuid components]
  ;; TODO validate that each component has all required component fns
  ;; implemented. Can we do this at compile time?
  {:uuid uuid
   :components components})

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
