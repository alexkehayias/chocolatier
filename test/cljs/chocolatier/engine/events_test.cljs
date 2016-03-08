(ns chocolatier.engine.events-test
  (:require [devcards.core :as dc :refer-macros [defcard deftest dom-node]]
            [cljs.test :refer-macros [is testing run-tests]]
            [chocolatier.engine.events :as ev]))

(defcard "# Event System Tests")

(deftest test-get-events
  (testing "Test getting events from the queue given selectors"
    (let [state {:state {:events {:queue {:a {:b [1 2] :c [3 4]}}}}}]
      (is (= :error (try (ev/get-events state []) (catch js/Error e :error)))
          "Invalid selector should throw an error")
      (is (= :error (try (ev/get-events state [:a]) (catch js/Error e :error)))
          "Invalid selector should throw an error")
      (is (= (ev/get-events state [:a :b]) [1 2]))
      (is (= (ev/get-events state [:a :c]) [3 4])))))

(deftest test-get-subscribed-events
  (testing "Test getting messages an entity is subscribed to"
    (let [queue {:x {:e1 [{:foo :bar}]}
                         :z {:e1 [{:baz :bat}]}
                 :y {:e2 [{:y :x}]}}]
      ;; JS arrays are never equivalent when using = so we must
      ;; convert them to clj data structures before testing
      (is (= (js->clj (ev/get-subscribed-events queue :e1 [:x :z]))
             (js->clj (array {:foo :bar} {:baz :bat})))))))

(deftest test-emit-event
  (testing "Test emitting events creates the expected entry in game state"
    (is (= (ev/emit-event {} {:foo :bar} [:a :b])
           {:state
            {:events
             {:queue
              {:a {:b [{:event-id :a :selectors [:a :b] :msg {:foo :bar}}]}}}}}))))
