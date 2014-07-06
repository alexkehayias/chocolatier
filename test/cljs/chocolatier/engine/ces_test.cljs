(ns chocolatier.engine.ces_test
  (:require-macros [cemerick.cljs.test
                    :refer [is deftest with-test run-tests testing test-var]])
  (:require [cemerick.cljs.test :as t]
            [chocolatier.engine.ces :as ces]))


(defn fixed-frame-game-loop
  "Test game loop 10 times and return the last state"
  [state system-spec count]
  ;; TODO based on the system spec, call each system in order
  (if (> count 10)
    state ;; break loop and return the result state
    (let [fns (for [[component-id & fn-keys] system-spec]
                #(ces/exec-system % component-id fn-keys))]
      (recur (ces/iter-fns state fns) system-spec (inc count)))))


(deftest test-game-loop-frame
  (let [test-state {;; Unique IDs of entities with components it implements
                    :entities {:player {:components [:testable]
                                        :meta {:human? true}}}
                    ;; Components for each entity that implements a component
                    :components {:player {:testable {}}}
                    :systems {:player {:testable
                                       {:test #(do (println "hello from test") %)
                                        :identity #(identity %)}}}}
        spec (ces/mk-system-spec [:testable :test :identity])]
    (fixed-frame-game-loop test-state test-system-spec 0)))

