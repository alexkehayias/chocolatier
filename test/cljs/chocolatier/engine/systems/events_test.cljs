(ns chocolatier.engine.systems.events-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [chocolatier.engine.systems.events :as ev]))


(deftest test-get-events
  (let [state {:state {:events {:queue {:a {:b [1 2] :c [3 4]}}}}}]
    (is (= :error (try (ev/get-events state []) (catch js/Error e :error)))
        "Invalid selector did not throw")
    (is (= :error (try (ev/get-events state [:a]) (catch js/Error e :error)))
        "Invalid selector did not throw")
    (is (= (ev/get-events state [:a :b]) [1 2]))
    (is (= (ev/get-events state [:a :c]) [3 4]))))

(deftest test-get-subscribed-events
  "Test getting messages for an entity that has nested and not nested subscriptions"
  (let [state {:state
               {:events
                {:queue {:x {:y [{:foo :bar}]}
                         :z [{:baz :bat}]
                         :y {:x [{:y :x}]}}
                 :subscriptions {:a [[:x :y] [:z]]}}}}]
    (is (= (ev/get-subscribed-events state [[:x :y] [:z]] )
           [{:foo :bar} {:baz :bat}]))))

(deftest test-emit-event
  (is (= (ev/emit-event {} {:foo :bar} [:a :b])
         {:state
          {:events
           {:queue
            {:a {:b [{:event-id :a :selectors [:a :b] :msg {:foo :bar}}]}}}}})))
