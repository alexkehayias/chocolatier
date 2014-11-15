(ns chocolatier.engine.test-events
  (:require-macros [cemerick.cljs.test :refer [is deftest]])
  (:require [cemerick.cljs.test :refer [test-ns]]
            [chocolatier.engine.systems.events :as ev]))

(deftest test-subscribe
  (is (= (ev/subscribe {} :my-event :me :my-component identity)
         {:state
          {:events
           {:subscriptions
            {:my-event
             {:me
              {:my-component identity}}}}}})))

(deftest test-msg->subscribers
  (let [queue [[:my-event :someone {:foo :bar}]]
        subscriptions {:my-event {:me {:my-component identity}}} 
        actual (ev/msg->subscribers queue subscriptions)]
    (is (= actual [[:my-event :someone {:foo :bar} :my-component :me]]))))

(deftest test-to-inbox
  "Test putting an event into an inbox"
  (let [actual (ev/to-inbox {} [:event :someone {} :comp :me])]
    (is (= actual
           {:state {:inbox {:comp {:me (seq [{:event-id :event
                                              :from :someone
                                              :msg {}}])}}}}))))

(deftest test-emit-event
  (is (= (ev/emit-event {} :test :me {:foo :bar})
         {:state {:events {:queue '([:test :me {:foo :bar}])}}})))

(deftest test-event-system
  "Test the event system sends all events to the inbox of subscribers"
  [state]
  (let [queue [[:my-event :someone {:foo :bar}]]
        subscriptions {:my-event {:me {:my-component identity}}}
        state {:state {:events {:queue queue :subscriptions subscriptions}}}
        inbox (-> (ev/event-system state) :state :inbox :my-component :me)]
    (is (= inbox
           (seq [{:event-id :my-event
                  :from :someone
                  :msg {:foo :bar}}])))))
