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

(def SCENES {})
(def STATE {})

(defn add-scene
  "Add or update existing scene in the scenes hashmap"
  [scenes-hm uid fns]
  (assoc scenes-hm uid fns))

(println "test add-scene" (add-scene SCENES :yo [identity]))

(defn iter-fns
  "Pass an initial value through a collection of functions with the 
   result of the function called is passed as an arg to the next
   function."
  [init fn-coll]
  ((apply comp fn-coll) init))

(println "test iter-fns" (iter-fns {} [#(assoc % :yo :dawg)]))

(defn game-loop
  [scenes scene-id state frame-count]
  (if (< frame-count 10)
    (recur scenes scene-id
           (iter-fns state (scene-id scenes))
           (inc frame-count))
    state))

(println "test game-loop" (game-loop {:yo [#(assoc % :yo :dawg)]} :yo {} 0))

(defn deep-merge
  "Recursively merges maps. If vals are not maps, the last value wins.
   via https://groups.google.com/forum/#!topic/clojure/UdFLYjLvNRs"
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(println "test deep-merge" (deep-merge {:a 1} {:b {:c 2}}))

(defn entities-with-component
  "Takes a hashmap and returns all keys whose values contain component-id"
  [entities component-id]
  (map first
       (filter #(boolean (some #{component-id} (second %))) entities)))

(println "test entities-with-component" (entities-with-component {:a [:b]} :b))

(defn mk-system-fn
  "Returns a function representing a system.

   Has two arrities:
   - [f] function f is called with state and wrapped to merge the return
     value of the function with state
   - [component-id f] function f is called with state and a collection of
     entity ids that match the given component id. Results are merged with
     state"
  ([f]
   (fn [state]
     (deep-merge state (f state (:entities state)))))  
  ([f component-id]
   (fn [state]
     (let [entities (entities-with-component component-id (:entities state))]
       (deep-merge state (f state entities))))))

(println "test mk-system-fn"
         ((mk-system-fn (fn [s ents] s) :b) {:entities {:a [:b]}}) )

(defn mk-entity
  "Adds entity with uid that has component-ids into state"
  [state uid component-ids]
  (assoc-in state [:entities uid] component-ids))

(println "test mk-entity"
         (mk-entity {} :player1 [:a :b]))

(defn get-component-state
  [state component-id entity-id]
  (get-in state [:components component-id :state entity-id]))

(defn mk-component-state
  "Returns a hashmap that can be merged into the state hashmap
   to store the state for the given component/entity id"
  [component-id entity-id val]
  {:components {component-id {:state {entity-id val}}}})

(println "test mk-component-state"
         (mk-component-state :foo :bar {}))

(defn mk-component-fn
  "Wraps function f with parameters for the component state for the
   corresponding entity. Return results of f are formatted so it can be
   merged into the game state."
  [component-id f]
  (fn [state entity-id]
    (mk-component-state
     component-id
     entity-id
     (f entity-id
        (get-component-state state component-id entity-id)))))

(println "test mk-component-fn"
         ((mk-component-fn :test (fn [& args] (identity {:foo "bar"})))
          {} :yo))

(defn mk-component
  "Returns an updated state hashmap with the given component
   Wraps each function in fns with mk-component-fn.

   The return value of any component function should be the updated
   component state which will be merged into the overall state"
  [state uid fns]
  (let [wrapped-fns (for [f fns] (mk-component-fn uid f))]
    (assoc state :components {uid {:fns fns :state {}}})))

(println "test mk-component"
         (mk-component {} :my-component [identity]))
