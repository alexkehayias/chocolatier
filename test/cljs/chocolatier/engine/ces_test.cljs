(ns chocolatier.engine.ces-test
  (:require-macros [cemerick.cljs.test
                    :refer [is deftest]])
  (:require [cemerick.cljs.test :refer [test-ns]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))


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
  (is (= (ces/mk-component-state {} :foo :bar {})
         {:state {:foo {:bar {}}}})))

(deftest test-mk-component-fn
  (let [f (ces/mk-component-fn :test (fn [& args] {:foo "bar"}))
        result (f {} :yo)]
    (is (= result {:state {:test {:yo {:foo "bar"}}}}))))

(deftest test-component-emits-events
  (let [event (ev/mk-event {:yo :dawg} :x :y)
        component-fn (fn [& args] [{:foo "bar"} [event]])
        f (ces/mk-component-fn :test component-fn)
        result (f {} :yo)]
    (is (= result {:state {:test {:yo {:foo "bar"}}
                           :events {:queue {:x {:y [event]}}}}}))))

(deftest test-mk-component-fn-with-options
  "Call mk-component-fn with the optional args-fn and format-fn to ensure
   it calls the component fn correctly."
  (let [;; The component fn takes a single argument, the state hashmap
        args-fn (fn [state component-id entity-id] [state])
        f (ces/mk-component-fn :test identity {:args-fn args-fn :format-fn identity})
        state {:foo :bar}
        actual (f state :test-entity-id)]
    (is (= actual state))))

(deftest test-mk-system
  (let [f (fn [s fns ents] "hi")
        sys-f (ces/mk-system-fn f :b)
        result (sys-f {:entities {:a [:b]} :components {:b {:fns [f]}}})]
    (is (= result "hi"))))

(defn game-loop
  "Simple game loop that runs 10 times and returns the state."
  [state scene-id frame-count]
  (if (< frame-count 10)
    (let [system-ids (-> state :scenes scene-id)
          fns (ces/get-system-fns state system-ids)
          updated-state (reduce #(%2 %1) state fns)]
      (recur updated-state scene-id (inc frame-count)))
    state))

(deftest test-integration
  "Test the entire CES implementation with a system that changes component state"
  []
  (let [;; Dummy test system iterates through component fns with state
        ;; and entity id
        test-system-fn (fn [state fns entity-ids]
                         (reduce #(%2 %1) state (for [f fns, e entity-ids] #(f % e))))
        test-fn (fn [entity-id component-state inbox]
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
    (is (= (-> result :state :testable)
           {:player1 {:x 10} :player2 {:x 10}}))))
