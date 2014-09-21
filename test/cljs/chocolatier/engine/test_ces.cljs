(ns chocolatier.engine.test-ces
  (:require-macros [cemerick.cljs.test
                    :refer [is deftest]])
  (:require [cemerick.cljs.test :as t]
            [chocolatier.engine.ces :as ces]))


(deftest test-iter-fns
  (is (= (ces/iter-fns 0 [inc inc inc]) 3)))

(deftest test-mk-scene
  (is (= (ces/mk-scene {} :yo [:dawg]) {:scenes {:yo [:dawg]}})))

(deftest test-deep-merge
  (is (= (ces/deep-merge {:a {:b 1}} {:a {:d 2}})
         {:a {:b 1 :d 2}})))

(deftest test-entities-with-component
  (is (= (vec (ces/entities-with-component {:a [:b] :b [:b]} :b))
         [:a :b])))

(deftest test-mk-entity
  (is (= (ces/mk-entity {} :player1 [:a :b])
         {:entities {:player1 [:a :b]}})))

(deftest test-mk-component-state
  (is (= (ces/mk-component-state :foo :bar {})
         {:components {:foo {:state {:bar {}}}}})))

(deftest test-mk-component-fn
  (let [f (ces/mk-component-fn :test (fn [& args] (identity {:foo "bar"})))
        result (f {} :yo)]
    (is (= result
           {:components {:test {:state {:yo {:foo "bar"}}}}}))))

(deftest test-mk-system
  (let [f (ces/mk-system-fn (fn [s fns ents] "hi") :b)
        result (f {:entities {:a [:b]}})]
    (is (= result "hi"))))

(defn game-loop
  "Simple game loop that runs 10 times and returns the state."
  [state scene-id frame-count]
  (if (< frame-count 10)
    (recur (ces/iter-fns state (ces/get-system-fns state (-> state :scenes scene-id)))
           scene-id
           (inc frame-count))
    state))

(deftest test-integration
  "Test the entire CES implementation with a system that changes component state"
  []
  (let [test-system-fn (fn [state fns entity-ids]
                         (apply ces/deep-merge (for [f fns, e entity-ids]
                                             (f state e))))
        test-fn (fn [component-state entity-id]
                  (println "testing" entity-id
                           component-state "->"
                           (assoc component-state :x
                                  (inc (or (:x component-state) 0))))
                  (assoc component-state :x (inc (or (:x component-state) 0))))
        init-state (-> {}
                       (ces/mk-scene :test-scene [:test-system])
                       (ces/mk-system :test-system test-system-fn :testable)
                       (ces/mk-entity :player1 [:testable])
                       (ces/mk-entity :player2 [:testable])
                       (ces/mk-component :testable [test-fn]))
        result (game-loop init-state :test-scene 0)]
    (is (= (-> result :components :testable :state)
           {:player1 {:x 10} :player2 {:x 10}}))))
