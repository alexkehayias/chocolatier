(ns chocolatier.engine.ces
  ;;(:require-macros [chocolatier.macros :refer [defentity
  ;defcomponent]])
  )

;; Gameloop:   recursive function that calls all systems in a scene
;; Scenes:     collection of systems used by the game loop
;; Systems:    functions that operate on components and return
;;             components
;;             takes a state object if it has any game impact
;; Entities:   unique IDs that have component implementations
;; Components: must return a new version of the state
;;             hold state relating to a certain aspect
;;             queryable based on a entity uuid
;;
;; State:      a nested hashmap
;;             {:testable
;;               {:player {:x 1 :y 2}
;;                :monster {:x 2 :y 3}}}
;;
;; Using one state hashmap should be performant enough even though it
;; create a new copy on every change to state in the game loop due to
;; structural sharing
;;
;; Questions:
;; Should systems only be allowed to change the state of other
;; components? How much sharing of state is needed to accomplish a the
;; purpose of a system

(defn iter-fns
  "Pass an initial value through a collection of functions with the 
   result of the function called is passed as an arg to the next
   function."
  [init coll]
  (reduce #(%2 %1) init coll))

(defn mk-component
  [name fields fns]
  {:fields fields
   :fns fns})

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

(defn implements?
  "Returns a boolean of whether the entity implements component"
  [state entity component]
  (boolean (some #{component} (-> state :entitities entity))))

(defn test-system
  "Call the test method for all Testable entities"
  [state]
  (let [ents (:entities state)
        entities (filter #(implements? :testable (second %)) ents)
        ids (map first entities)]
    ;; Since each protocol returns a new state, we can iterate through
    ;; all by using iter-fns an the test method
    (iter-fns state (for [i ids] (partial test i)))))

;; Example game state
(def test-state
  {;; Unique IDs of entities with components it implements
   :entities {:player {:components [:testable]
                       :meta {:human? true}}}
   ;; Components for each entity that implements a component
   :components {:player {:testable {}}}
   :systems {:player {:testable {:test #(do (println "hello from test") %)
                                 :identity #(identity %)}}}}
  )

;; FIX this will through a null pointer if there is no
;; matching system for the entity

;; This only operates on entities, can a system work on something
;; other than entities? What about game meta data?
(defn exec-system
  "Execute the component by calling each component function in order."
  [state component-id fn-keys]
  (let [;; Get all entities that implement this component
        entities (filter #(some #{component-id} (:components (second %)))
                         (seq (:entities state)))
        ;; Grab the ids
        ids (map first entities)
        ;; Get the component implementation function map for the system
        fn-maps (map #(get-in (:systems state) [% component-id]) ids)
        ;; Make a sequence of all the functions to call in order as
        ;; they were passed to this function
        fns (reduce into [] (for [fk fn-keys] (map fk fn-maps)))]
    (iter-fns state fns)))

(defn test-game-loop
  "Test game loop 10 times and return the last state"
  [state system-spec count]
  ;; TODO based on the system spec, call each system in order
  (if (> count 10)
    state ;; break loop and return the result state
    (let [fns (for [[component-id & fn-keys] system-spec]
                #(exec-system % component-id fn-keys))]
      (recur (iter-fns state fns) system-spec (inc count)))))


(defn mk-system-spec
  "Convenience wrapper so you don't have to specify vectors of vectors"
  [& specs] specs)

(def test-system-spec
  (mk-system-spec [:testable :test :identity]))

;; Test
(test-game-loop test-state test-system-spec 0)


;; Can also be called without an initial state
;; (game-loop {} [test-system] 0)

;; Or without any component state
;; (game-loop {:entities [(new Entity :player)]} [test-system] 0)


;; Macros
;; defentity
;; Creates a defrecord with a list of methods and adds all the
;; required state as namespaced keywords on the record
;; :comp-id/:var
;; :moveable/:x 1 :moveable/:y 1
;; That way all component state for an entity can be accessed in
;; methods via `this`
;; Global state can then be a list of entities and systems

;; (defentity Player
;;   Movable
;;   (move [:x :y])
;;   (other-method [this state] nil)

;; (defcomponent Moveable [:x :y])
;; (defprotocol Moveable [this state])


;; (defcomponent Moveable
;;   (move [this state component]))

;; (defmacro defentity
;;   [vname methods]
;;   `(defrecord ~vname
;;        `(for [m methods]
;;           m)))
