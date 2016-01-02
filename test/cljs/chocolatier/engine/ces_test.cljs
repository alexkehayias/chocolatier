(ns chocolatier.engine.ces-test
  (:require [cljs.test :refer-macros [deftest is run-tests]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))


(deftest test-mk-scene
  (is (= (ces/mk-scene {} :yo [:dawg]) {:scenes {:yo [:dawg]}})))

(deftest test-entities-with-component
  (let [state (-> {}
                  (ces/mk-entity :e1 [:c1 :c2])
                  (ces/mk-entity :e2 [:c2]))]
    (is (= [:e1] (ces/entities-with-component state :c1)))
    (is (= [:e1 :e2] (ces/entities-with-component state :c2)))
    (is (= [] (ces/entities-with-component state :c3)))))

(deftest test-mk-entity
  "Test the output shape of mk-entity"
  (is (= {:entities {:e1 '(:c2 :c1)}
          :state {:c1 {:e1 {}} :c2 {:e1 {}}}}
         (ces/mk-entity {} :e1 [:c1 :c2]))))

(deftest test-mk-component-state
  "Test the output shape of mk-component-state"
  (is (= {:state {:foo {:bar {}}}}
         (ces/mk-component-state {} :foo :bar {}))))

(deftest test-mk-component-fn
  "Test that calling mk-component-fn with expected arguments returns a new state.
   - Component functions can be called with an arrity of 2 or 3
   - When called with 3 args, the last arg should be merged into the third
     argument of the component function being wrapped
   - When the wrapped function returns a hashmap, the hashmap should merged into
     the game state
   - When the wrapped function returns a vector of a hashmap and coll of events
     the hashmap should be merged into the game state and events should be in the
     queue"
  (let [f1 (ces/mk-component-fn :c1 (fn [entity-id component-state context]
                                      (assoc component-state :id entity-id)))
        f2 (ces/mk-component-fn :c1 (fn [entity-id component-state context]
                                      (assoc component-state :context context)))
        f3 (ces/mk-component-fn :c1 (fn [entity-id component-state context]
                                      [component-state
                                       [(ev/mk-event {:foo :bar} [:q1])]]))
        ]
    (is (= {:state {:c1 {:e1 {:id :e1}}}} (f1 {} :e1)))
    (is (= {:state {:c1 {:e1 {:context {:foo :bar}}}}} (f2 {} :e1 {:foo :bar})))
    (is (= {:state {:c1 {:e1 {}}
                    :events {:queue
                             {:q1 '({:event-id :q1 :selectors [:q1]
                                     :msg {:foo :bar}})}}}}
           (f3 {} :e1)))))

(deftest test-component-emits-events
  (let [event (ev/mk-event {:yo :dawg} [:x :y])
        component-fn (fn [& args] [{:foo "bar"} [event]])
        f (ces/mk-component-fn :test component-fn)
        result (f {} :yo)]
    (is (= {:state {:test {:yo {:foo "bar"}}
                    :events {:queue {:x {:y [event]}}}}}
           result ))))

(deftest test-mk-component-fn-with-options
  "Call mk-component-fn with the optional args-fn and format-fn to ensure
   it calls the component fn correctly."
  (let [;; The component fn takes a single argument, the state hashmap
        args-fn (fn [state component-id entity-id] {:state state})
        f (ces/mk-component-fn :c1 (fn [_ _ context] context)
                               {:args-fn args-fn :format-fn identity})
        state {:state {:foo :bar}}]
    (is (= state (f state :e1)))))

;; TODO need to test all the arrities
;; (deftest test-mk-system
;;   (let [f (fn [s fns ents] "hi")
;;         sys-f (ces/mk-system-fn f :b)
;;         result (sys-f {:entities {:a [:b]} :components {:b {:fns [f]}}})]
;;     (is (= result "hi"))))

;; (defn game-loop
;;   "Simple game loop that runs 10 times and returns the state."
;;   [state scene-id frame-count]
;;   (if (< frame-count 10)
;;     (let [system-ids (-> state :scenes scene-id)
;;           fns (ces/get-system-fns state system-ids)
;;           updated-state (reduce #(%2 %1) state fns)]
;;       (recur updated-state scene-id (inc frame-count)))
;;     state))

;; (deftest test-integration
;;   "Test the entire CES implementation with a system that changes component state"
;;   []
;;   (let [;; Dummy test system iterates through component fns with state
;;         ;; and entity id
;;         test-system-fn (fn [state fns entity-ids]
;;                          (reduce #(%2 %1) state (for [f fns, e entity-ids] #(f % e))))
;;         test-fn (fn [entity-id component-state inbox]
;;                   (println "testing" entity-id
;;                            component-state "->"
;;                            (assoc component-state :x
;;                                   (inc (or (:x component-state) 0))))
;;                   (assoc component-state :x (inc (or (:x component-state) 0))))
;;         init-state (-> {}
;;                        (ces/mk-scene :test-scene [:test-system])
;;                        (ces/mk-system :test-system test-system-fn :testable)
;;                        (ces/mk-entity :player1 [:testable])
;;                        (ces/mk-entity :player2 [:testable])
;;                        (ces/mk-component :testable [test-fn]))
;;         result (game-loop init-state :test-scene 0)]
;;     (is (= (-> result :state :testable)
;;            {:player1 {:x 10} :player2 {:x 10}}))))
