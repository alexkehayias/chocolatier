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

(println "test add-scene" (mk-scene {} :yo [identity]))

(defn iter-fns
  "Pass an initial value through a collection of functions with the 
   result of the function called is passed as an arg to the next
   function."
  [init fn-coll]
  ((apply comp fn-coll) init))

(println "test iter-fns" (iter-fns {} [#(assoc % :yo :dawg)]))

(defn get-system-fns
  "Return system functions with an id that matches system-ids in order."
  [state system-ids]
  (map #(% (:systems state)) system-ids))

(defn game-loop
  [state scene-id frame-count]
  (println (get-system-fns state (-> state :scenes scene-id)))
  (println state)
  (if (< frame-count 10)
    (recur (iter-fns state (get-system-fns state (-> state :scenes scene-id)))
           scene-id
           (inc frame-count))
    state))

(println "test game-loop" (game-loop {:yo [#(assoc % :yo :dawg)]} :yo 0))

(defn deep-merge
  "Recursively merges maps. If vals are not maps, the last value wins.
   via https://groups.google.com/forum/#!topic/clojure/UdFLYjLvNRs"
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(println "test deep-merge" (deep-merge {:a {:b 1}} {:a {:d 2}}))

(defn entities-with-component
  "Takes a hashmap and returns all keys whose values contain component-id"
  [entities component-id]
  (map first
       (filter #(boolean (some #{component-id} (second %))) entities)))

(println "test entities-with-component"
         (entities-with-component {:a [:b] :b [:b]} :b))

(defn mk-entity
  "Adds entity with uid that has component-ids into state"
  [state uid component-ids]
  (assoc-in state [:entities uid] component-ids))

(println "test mk-entity"
         (mk-entity {} :player1 [:a :b]))

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

(println "test mk-component-state"
         (mk-component-state :foo :bar {}))

(defn mk-component-fn
  "Wraps function f with parameters for the component state for the
   corresponding entity. Return results of f are formatted so it can be
   merged into the game state."
  [component-id f]
  (fn [state entity-id]
    (let [r (mk-component-state
             component-id
             entity-id
             (f (get-component-state state component-id entity-id)
                entity-id))]
      (println "component-fn result" r)
      r)))

(println "test mk-component-fn"
         ((mk-component-fn :test (fn [& args] (identity {:foo "bar"})))
          {} :yo))

(defn mk-component
  "Returns an updated state hashmap with the given component.
   Wraps each function in fns with mk-component-fn.

   The return value of any component function should be the updated
   component state which will be merged into the overall state"
  [state uid fns]
  (let [wrapped-fns (for [f fns] (mk-component-fn uid f))]
    (assoc state :components {uid {:fns wrapped-fns :state {}}})))

(println "test mk-component"
         (mk-component {} :my-component [identity]))

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
     (deep-merge state (f state (:entities state)))))  
  ([f component-id]
   (fn [state]
     (let [entities (entities-with-component (:entities state) component-id)
           component-fns (get-component-fns state component-id)]
       (deep-merge state (f state component-fns entities))))))

(println "test mk-system-fn"
         ((mk-system-fn (fn [s fns ents] s) :b) {:entities {:a [:b]}}))

(defn mk-system
  "Add the system function to the state. Wraps the system function using
   mk-system-fn. Returns an update hashmap."
  [state uid f & [component-id]]
  (let [system-fn (if component-id
                    (mk-system-fn f component-id)
                    (mk-system-fn f))]
    (assoc-in state [:systems uid] system-fn)))

(defn integration-test
  "Test the entire CES implementation with a system that changes component state"
  []
  (let [test-system-fn (fn [state fns entity-ids]
                         (apply deep-merge (for [f fns, e entity-ids]
                                             (f state e))))
        test-fn (fn [component-state entity-id]
                  (println "testing" entity-id
                           component-state "->"
                           (assoc component-state :x
                                  (inc (or (:x component-state) 0))))
                  (assoc component-state :x (inc (or (:x component-state) 0))))
        state (-> {}
                  (mk-scene :test-scene [:test-system])
                  (mk-system :test-system test-system-fn :testable)
                  (mk-entity :player1 [:testable])
                  (mk-entity :player2 [:testable])
                  (mk-component :testable [test-fn]))]
    (game-loop state :test-scene 0)))

(println "Running integration-test" (integration-test))
