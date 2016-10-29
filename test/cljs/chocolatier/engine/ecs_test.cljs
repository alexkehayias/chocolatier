(ns chocolatier.engine.ecs-test
  (:require [cljs.test :refer-macros [is testing run-tests]]
            [devcards.core :as dc :refer-macros [defcard deftest dom-node]]
            [chocolatier.engine.ecs :as ecs]
            [chocolatier.engine.events :as ev]))


(defcard "# Entity Component System Tests")

(deftest test-mk-scene
  (testing "Test mk-scene returns the correct output"
    (is (= (ecs/mk-scene {} {:uid :yo :systems [:dawg]}) {:scenes {:yo [:dawg]}}))))

(deftest test-entities-with-component
  (testing "Test the expected entity IDs are returned"
    (let [state (-> {}
                    (ecs/mk-entity {:uid :e1
                                    :components [{:uid :c1}
                                                 {:uid :c2}]})
                    (ecs/mk-entity {:uid :e2
                                    :components [{:uid :c2}]}))]
      (is (= #{:e1} (ecs/entities-with-component state :c1)))
      (is (= #{:e1 :e2} (ecs/entities-with-component state :c2)))
      (is (= #{} (ecs/entities-with-component state :c3))))))

(deftest test-entities-with-multi-components
  (testing "Test the expected entity IDs are returned when asking for entities
           with multiple components"
    (let [state (-> {}
                    (ecs/mk-entity {:uid :e1
                                    :components [{:uid :c1}
                                                 {:uid :c2}]})
                    (ecs/mk-entity {:uid :e2
                                    :components [{:uid :c3}]}))]
      (is (= #{:e1} (ecs/entities-with-multi-components state [:c1 :c2])))
      (is (= #{:e2} (ecs/entities-with-multi-components state [:c3])))
      (is (= #{} (ecs/entities-with-multi-components state [:c4]))))))

(deftest test-mk-entity
  (testing "Test the output shape of mk-entity"
    (is (= {:entities {:e1 #{:c1 :c2}}
            :components {:c1 {:entities #{:e1}} :c2 {:entities #{:e1}}}
            :state {:c1 {:e1 {}} :c2 {:e1 {:x 0}}}}
           (ecs/mk-entity {} {:uid :e1
                              :components [{:uid :c1}
                                           {:uid :c2 :state {:x 0}}]})))))

(deftest rm-entity-from-component-index
  (testing "Test the entity is removed from the component entity index"
    (let [state (ecs/mk-entity {}
                               {:uid :e1
                                :components [{:uid :c1 :state {:x 0}}
                                             {:uid :c2 :state {:y 0}}]})]
      (is (= {:entities {:e1 [:c1 :c2]}
              :components {:c1 {:entities #{}} :c2 {:entities #{}}}
              :state {:c1 {:e1 {:x 0}} :c2 {:e1 {:y 0}}}})))))

(deftest test-rm-entity
  (testing "Test removing an entity cleans it out of the game state"
    (let [state (ecs/mk-entity {} {:uid :e1
                                   :components [{:uid :c1 :state {:x 0}}
                                                {:uid :c2 :state {:y 0}}]})]
      (is (= {:entities {}
              :components {:c1 {:entities #{}} :c2 {:entities #{}}}
              :state {:c1 {} :c2 {}}}
             (ecs/rm-entity state {:uid :e1}))))))

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
                    (ecs/mk-system {:uid :s1
                                    :component {:uid :c1
                                                :fn component-fn}})
                    (ecs/mk-entity {:uid :e1
                                   :components [{:uid :c1 :state {:x 0}}]})
                    (ecs/mk-entity {:uid :e2
                                   :components [{:uid :c1 :state {:x 1}}]})
                    ;; An entity that does not participate
                    (ecs/mk-entity {:uid :e3
                                   :components [{:uid :c2 :state {:foo :bar}}]}))
          system-fn (get-in state [:systems :s1])]
      (is (= {:e1 {:x 1} :e2 {:x 2}}
             (ecs/get-all-component-state (system-fn state) :c1)))))
  ^{:key :t2}
  (testing "Test the system outputs the correct events when called"
    (let [component-fn (fn [entity-id component-state context]
                         [{} [(ev/mk-event {:id entity-id} [:c1 entity-id])]])
          state (-> {}
                    (ecs/mk-system {:uid :s1
                                    :component {:uid :c1
                                                :fn component-fn}})
                    (ecs/mk-entity {:uid :e1
                                    :components [{:uid :c1}]})
                    (ecs/mk-entity {:uid :e2
                                   :components [{:uid :c1}]})                    )
          system-fn (get-in state [:systems :s1])
          next-state (system-fn state)
          queue (get-in next-state ev/queue-path)]
      (is (= [(ev/mk-event {:id :e1} [:c1 :e1])]
             (js->clj (ev/get-subscribed-events queue :e1 [:c1]))))
      (is (= [(ev/mk-event {:id :e2} [:c1 :e2])]
             (js->clj (ev/get-subscribed-events queue :e2 [:c1])))))))

(deftest test-mk-system
  ^{:key :t1}
  (testing "Test the creation of a custom system function"
    (is (= {:systems {:s1 identity}} (ecs/mk-system {} {:uid :s1 :fn identity}))))
  ^{:key :t2}
  (testing "Test the creation of a system function that operates on a component also creates the component in the state"
    (let [component-fn (fn [entity-id component-state context]
                         (update component-state :x inc))
          state (ecs/mk-system {} {:uid :s1
                                   :component {:uid :c1
                                               :fn component-fn}})]
      (is (contains? (:systems state) :s1))
      (is (map? (ecs/get-component state :c1))))))

(deftest test-get-component-context
  (testing "Test the correct context is returned for updating a component for a given entity"
    (let [state (-> {}
                    (ecs/mk-system {:uid :s1
                                    :component {:uid :c1
                                                :fn identity
                                                :select-components [:c2]
                                                :subscriptions [:m1]}})
                    (ecs/mk-entity {:uid :e1
                                    :components [{:uid :c1 :state {:x 0}}
                                                 {:uid :c2 :state {:y 0}}]})
                    (ev/emit-event {:foo :bar} [:m1 :e1]))
          component (ecs/get-component state :c1)
          queue (get-in state ev/queue-path)
          context (ecs/get-component-context state queue component :e1)]
      (is (= {:foo :bar} (-> context :inbox first :msg)))
      (is (= {:y 0} (:c2 context))))))
