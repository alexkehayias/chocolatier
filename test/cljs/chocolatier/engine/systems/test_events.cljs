(ns chocolatier.engine.test-events
  (:require-macros [cemerick.cljs.test :refer [is deftest]])
  (:require [cemerick.cljs.test :refer [test-ns]]
            [chocolatier.engine.systems.events :as ev]))

(deftest test-subscribe
  (is (= (ev/subscribe {} :me :my-event :some-source)
         {:state
          {:events
           {:subscriptions {:me [[:my-event :some-source]]} }}})))

(deftest test-get-subscribed-events
  "Test getting messages for an entity that has nested and not nested subscriptions"
  (let [state {:state
               {:events
                {:queue {:x {:y [{:event-id :x}]}
                         :z [{:event-id :broadcast}]
                         :y {:x [{:event-id :x}]}}
                 :subscriptions {:a [[:x :y] :z]}}}}]
    (is (= (ev/get-subscribed-events state :a)
           [{:event-id :x} {:event-id :broadcast}]))))

(deftest test-emit-event
  (is (= (ev/emit-event {} {:foo :bar} :a :b)
         {:state {:events {:queue {:a {:b [{:selectors [:a :b] :msg {:foo :bar}}]}}}}})))
