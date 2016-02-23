(ns chocolatier.engine.ecs-test
  (:require [cljs.test :refer-macros [is testing run-tests]]
            [devcards.core :as dc :refer-macros [defcard deftest dom-node]]
            [chocolatier.engine.ecs :as ecs]
            [chocolatier.engine.events :as ev]))


(defcard "# Entity Component System Tests")

(deftest test-mk-scene
  (testing "Test mk-scene returns the correct output"
    (is (= (ecs/mk-scene {} :yo [:dawg]) {:scenes {:yo [:dawg]}}))))

(deftest test-entities-with-component
  (testing "Test the expected entity IDs are returned"
    (let [state (-> {}
                    (ecs/mk-entity :e1 [:c1 :c2])
                    (ecs/mk-entity :e2 [:c2]))]
      (is (= [:e1] (ecs/entities-with-component state :c1)))
      (is (= [:e1 :e2] (ecs/entities-with-component state :c2)))
      (is (= [] (ecs/entities-with-component state :c3))))))

(deftest test-mk-entity
  (testing "Test the output shape of mk-entity"
    (is (= {:entities {:e1 '(:c2 :c1)}
            :state {:c1 {:e1 {}} :c2 {:e1 {}}}}
           (ecs/mk-entity {} :e1 [:c1 :c2])))))

(deftest test-mk-component-state
  (testing "Test the output shape of mk-component-state"
    (is (= {:state {:foo {:bar {}}}}
           (ecs/mk-component-state {} :foo :bar {})))))

(deftest test-mk-component-fn
  (testing
      "Test that calling mk-component-fn with expected arguments returns a new state.
       - Component functions can be called with an arrity of 2 or 3
       - When called with 3 args, the last arg should be merged into the third
         argument of the component function being wrapped
       - When the wrapped function returns a hashmap, the hashmap should merged into
         the game state
       - When the wrapped function returns a vector of a hashmap and coll of events
         the hashmap should be merged into the game state and events should be in the
         queue"
    (let [f1 (ecs/mk-component-fn :c1 (fn [entity-id component-state context]
                                        (assoc component-state :id entity-id)))
          f2 (ecs/mk-component-fn :c1 (fn [entity-id component-state context]
                                        (assoc component-state :context context)))
          f3 (ecs/mk-component-fn :c1 (fn [entity-id component-state context]
                                        [component-state
                                         [(ev/mk-event {:foo :bar} [:q1])]]))]
      (is (= {:state {:c1 {:e1 {:id :e1}}}} (f1 {} :e1)))
      (is (= {:state {:c1 {:e1 {:context {:foo :bar}}}}} (f2 {} :e1 {:foo :bar})))
      (is (= {:state {:c1 {:e1 {}}
                      :events {:queue
                               {:q1 '({:event-id :q1 :selectors [:q1]
                                       :msg {:foo :bar}})}}}}
             (f3 {} :e1))))))

(deftest test-component-emits-events
  (testing "Test a component fn can emit events when by returning a two element vector"
    (let [event (ev/mk-event {:yo :dawg} [:x :y])
          component-fn (fn [& args] [{:foo "bar"} [event]])
          f (ecs/mk-component-fn :test component-fn)
          result (f {} :yo)]
      (is (= {:state {:test {:yo {:foo "bar"}}
                      :events {:queue {:x {:y [event]}}}}}
             result )))))

(deftest test-mk-component-fn-with-options
  (testing "Test optional args-fn and format-fn to ensure it calls the component fn correctly."
    (let [ ;; The component fn takes a single argument, the state hashmap
          args-fn (fn [state component-id entity-id] {:state state})
          f (ecs/mk-component-fn :c1 (fn [_ _ context] context)
                                 {:args-fn args-fn :format-fn identity})
          state {:state {:foo :bar}}]
      (is (= state (f state :e1))))))
