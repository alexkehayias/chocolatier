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
      (is (= #{:e1} (ecs/entities-with-component state :c1)))
      (is (= #{:e1 :e2} (ecs/entities-with-component state :c2)))
      (is (= #{} (ecs/entities-with-component state :c3))))))

(deftest test-entities-with-multi-components
  (testing "Test the expected entity IDs are returned when asking for entities
           with multiple components"
    (let [state (-> {}
                    (ecs/mk-entity :e1 [:c1 :c2])
                    (ecs/mk-entity :e2 [:c3]))]
      (is (= #{:e1} (ecs/entities-with-multi-components state [:c1 :c2])))
      (is (= #{:e2} (ecs/entities-with-multi-components state [:c3])))
      (is (= #{} (ecs/entities-with-multi-components state [:c4]))))))

(deftest test-mk-entity
  (testing "Test the output shape of mk-entity"
    (is (= {:entities {:e1 #{:c2 :c1}}
            :components {:c1 {:entities #{:e1}} :c2 {:entities #{:e1}}}
            :state {:c1 {:e1 {}} :c2 {:e1 {:x 0}}}}
           (ecs/mk-entity {} :e1 [:c1 [:c2 {:x 0}]])))))

(deftest test-rm-entity
  (testing "Test removing an entity cleans it out of the game state"
    (let [state (ecs/mk-entity {} :e1 [:c1 :c2])]
      (is (= {:entities {}
              :components {:c1 {:entities #{}} :c2 {:entities #{}}}
              :state {:c1 {} :c2 {}}}
             (ecs/rm-entity state :e1))))))

(deftest test-mk-component-state
  (testing "Test the output shape of mk-component-state"
    (is (= {:state {:foo {:bar {}}}}
           (ecs/mk-component-state {} :foo :bar {})))))
