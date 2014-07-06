(ns chocolatier.engine.ces_test
  (:require-macros [cemerick.cljs.test
                    :refer [is deftest with-test run-tests testing test-var]])
  (:require [cemerick.cljs.test :as t]
            [chocolatier.engine.ces :as ces]))


(deftest test-iter-fns
  "Test iter-fns does what we expect"
  (is (= (ces/iter-fns 0 [inc inc inc]) 3)))

(defn test-system
  "Call the test method for all Testable entities"
  [state]
  (let [ents (:entities state)
        entities (filter #(implements? :testable (second %)) ents)
        ids (map first entities)]
    ;; Since each protocol returns a new state, we can iterate through
    ;; all by using iter-fns an the test method
    (iter-fns state (for [i ids] (partial identity i)))))

(defn fixed-frame-game-loop
  "Simple game loop that is called n times and returns the last game state."
  [state system-spec frame-count limit]
  (if (>= frame-count limit)
    state ;; break loop and return the result state
    (let [fns (for [[component-id fn-keys] system-spec]
                #(ces/exec-system % component-id fn-keys))]
      (recur (ces/iter-fns state fns) system-spec (inc frame-count) limit))))

(deftest test-one-frame
  (let [test-fn #(do (println "hello from test") %)
        test-state { ;; Unique IDs of entities with components it implements
                    :entities {:player {:components [:testable]
                                        ;; Any other meta info about
                                        ;; the entity we wish to keep
                                        :meta {:human? true}}}
                    ;; Components for each entity that implements a component
                    :components {:player {:testable {}}}
                    ;; Functions that operate on a component
                    :systems {:player {:testable
                                       {:test test-fn :identity #(identity %)}}}}
        test-system-spec (ces/mk-system-spec [:testable [:test :identity]])
        result (fixed-frame-game-loop test-state test-system-spec 0 1)]
    ;; Make sure the state is a hashmap
    (is (= (type result) (type {})))
    ;; Make sure we didn't lose any keys
    (is (= (set (keys result)) #{:entities :components :systems}))
    ;; Make sure we didn't lose an entity
    (is (= (set (keys (:entities result))) #{:player}))))
