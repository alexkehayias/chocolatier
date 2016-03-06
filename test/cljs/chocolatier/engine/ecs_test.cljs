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

(deftest rm-entity-from-component-index
  (testing "Test the entity is removed from the component entity index"
    (let [state (ecs/mk-entity {} :e1 [[:c1 {:x 0}] [:c2 {:y 0}]])]
      (is (= {:entities {:e1 [:c1 :c2]}
              :components {:c1 {:entities #{}} :c2 {:entities #{}}}
              :state {:c1 {:e1 {:x 0}} :c2 {:e1 {:y 0}}}})))))

(deftest test-rm-entity
  (testing "Test removing an entity cleans it out of the game state"
    (let [state (ecs/mk-entity {} :e1 [[:c1 {:x 0}] [:c2 {:y 0}]])]
      (is (= {:entities {}
              :components {:c1 {:entities #{}} :c2 {:entities #{}}}
              :state {:c1 {} :c2 {}}}
             (ecs/rm-entity state :e1))))))

(deftest test-mk-component-state
  (testing "Test the output shape of mk-component-state"
    (is (= {:state {:foo {:bar {}}}}
           (ecs/mk-component-state {} :foo :bar {})))))

(deftest test-concat-keywords
  (testing "Test joining multiple keywords into a new one"
    (is (= :a-b (ecs/concat-keywords :a :b)))))

(deftest test-mk-system-fn
  ^{:key :t1}
  (testing "Test the system outputs the correct component states when called"
    (let [component-fn (fn [entity-id component-state context]
                         (update component-state :x inc))
          state (-> {}
                    (ecs/mk-system :s1 :c1 component-fn)
                    (ecs/mk-entity :e1 [[:c1 {:x 0}]])
                     (ecs/mk-entity :e2 [[:c1 {:x 1}]])
                    ;; An entity that does not participate
                    (ecs/mk-entity :e3 [[:c2 {:foo :bar}]]))
          system-fn (get-in state [:systems :s1])]
      (is (= {:e1 {:x 1} :e2 {:x 2}}
             (ecs/get-all-component-state (system-fn state) :c1)))))
  ^{:key :t2}
  (testing "Test the system outputs the correct events when called"
    (let [component-fn (fn [entity-id component-state context]
                         [{} [(ev/mk-event {:id entity-id} [:c1 entity-id])]])
          state (-> {}
                    (ecs/mk-system :s1 :c1 component-fn)
                    (ecs/mk-entity :e1 [:c1])
                    (ecs/mk-entity :e2 [:c1]))
          system-fn (get-in state [:systems :s1])
          next-state (system-fn state)]
      (is (= [(ev/mk-event {:id :e1} [:c1 :e1])]
             (js->clj (ev/get-subscribed-events next-state :e1 [:c1]))))
      (is (= [(ev/mk-event {:id :e2} [:c1 :e2])]
             (js->clj (ev/get-subscribed-events next-state :e2 [:c1])))))))

(deftest test-mk-system
  ^{:key :t1}
  (testing "Test the creation of a custom system function"
    (is (= {:systems {:s1 identity}} (ecs/mk-system {} :s1 identity))))
  ^{:key :t2}
  (testing "Test the creation of a system function that operates on a component also creates the component in the state"
    (let [component-fn (fn [entity-id component-state context]
                         (update component-state :x inc))
          state (ecs/mk-system {} :s1 :c1 identity)]
      (is (contains? (:systems state) :s1))
      (is (map? (ecs/get-component state :c1))))))

(deftest test-get-component-context
  (testing "Test the correct context is returned for updating a component for a given entity"
    (let [state (-> {}
                    (ecs/mk-system :s1 :c1 [identity {:select-components [:c2]
                                                      :subscriptions [:m1]}])
                    (ecs/mk-entity :e1 [[:c1 {:x 0}] [:c2 {:y 0}]])
                    (ev/emit-event {:foo :bar} [:m1 :e1]))
          component (ecs/get-component state :c1)
          context (ecs/get-component-context state :e1 component)]
      (is (= {:foo :bar} (-> context :inbox first :msg)))
      (is (= {:y 0} (:c2 context))))))
