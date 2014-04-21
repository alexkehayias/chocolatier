(ns chocolatier.engine.ces)

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

;; Using one state hashmap should be performant enough even though it
;; create a new copy on every change to state in the game loop
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

(defn game-loop
  "Test game loop 10 times and return the last state"
  [state systems]
  (loop [c 0
         s state]
    (if (> c 10)
      s ;; break loop and return the result state
      (recur (inc c) (iter-fns s systems)))))

(defprotocol Testable
  (test [this state]))

(defrecord Entity [id]
  Testable
  (test [this state]
    (let [id (:id this)
          ;; If you don't get the id from this the treading macro
          ;; returns `this` instead of the keyword function result
          ;; Seems like a bug in cljs
          comp (-> state :components id :testable)
          updated-comp (assoc comp :x (inc (:x comp)))]
      (assoc-in state [:components id :testable] updated-comp))))

(defn test-system
  "Call the test method for all Testable entities"
  [state]
  (let [ents (:entities state)
        entities (filter #(satisfies? Testable %) ents)]
    ;; Since each protocol returns a new state, we can iterate through
    ;; all by using iter-fns an the test method
    (iter-fns state (for [e entities] (partial test e)))))

;; Test
;; (game-loop {:entities [(new Entity :player)]
;;             :components {:player {:testable {:x 1}}}} [test-system])

;; Can also be called without an initial state
;; (game-loop {} [test-system])

;; Or without any component state
;; (game-loop {:entities [(new Entity :player)]} [test-system])



