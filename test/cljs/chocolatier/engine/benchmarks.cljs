(ns chocolatier.engine.benchmarks
  "Basic benchmarks for measuring frames per second for the engine"
  (:require [chocolatier.engine.ces :as ces]
            [chocolatier.engine.core :refer [mk-game-state
                                             timestamp]]
            [chocolatier.engine.systems.events :refer [mk-event]])
  (:require-macros [chocolatier.macros :refer [forloop local >> <<]]))


(defn game-loop
  [game-state]
  (let [scene-id (get-in game-state [:game :scene-id])
        systems (ces/get-system-fns game-state (-> game-state :scenes scene-id))
        state (local game-state)]
    (forloop [[i 0] (< i (count systems)) (inc i)]
             (>> state ((systems i) (<< state))))
    (<< state)))

(defn system-fn
  [state fns entity-ids]
  (ces/iter-entities state fns entity-ids))

(defn component-fn-state-change-only
  "Only updates the component state, does not emit any events"
  [eid cmp-state events]
  (update-in cmp-state [:x] inc))

(defn component-fn-state-change-and-events
  "Updates the component state and emits events"
  [eid cmp-state events]
  [(update-in cmp-state [:x] inc)
   [(mk-event {:testing "hi"} eid :ev1)]])

(def simple
  "Returns game state for a simple single system, single component, single entity"
  (mk-game-state {}
                 :default
                 [:scene :default [:s1]]
                 [:component :c1 [component-fn-state-change-only]]
                 [:system :s1 system-fn :c1]
                 [:entity :e1 :components [:c1] :subscriptions [[:e1 :ev1]]]))

(def many-entities
  "Returns game state for a simple single system, single component, single entity"
  (let [entities (doall
                  (map #(vector :entity (keyword (str "e" %))
                                :components [:c1]
                                :subscriptions [[(keyword (str "e" %))
                                                 (keyword (str "ev" %))]])
                         (range 1000)))
        args (concat [{}
                      :default
                      [:scene :default [:s1]]
                      [:component :c1 [component-fn-state-change-only]]
                      [:system :s1 system-fn :c1]]
                     entities)]
    (apply mk-game-state args)))

(def many-systems
  "Returns a game state with 100 entities 100 components and 100 systems"
  (let [entities (doall
                  (map #(vector :entity (keyword (str "e" %))
                                :components (doall (map (fn [n] (keyword (str "c" n)))
                                                        (range 100)))
                                :subscriptions [[(keyword (str "e" %))
                                                 (keyword (str "ev" %))]])
                       (range 100)))
        systems (doall
                 (map #(vector :system (keyword (str "s" %))
                               system-fn (keyword (str "c" %)))
                      (range 100)))
        components (doall
                    (map #(vector :component (keyword (str "c" %))
                                  [component-fn-state-change-only])
                         (range 100)))
        scene [:scene :default (doall (map #(keyword (str "s" %)) (range 100)))]
        args (concat [{} :default scene]
                     systems
                     components
                     entities)]
    (apply mk-game-state args)))

(defn run-benchmark
  "Calculates the number of times per second the function can be called"
  [game-state runs]
  (println "Running benchmark best of" runs)
  (let [results (atom [])]
    (dotimes [x runs]
      (let [frame-counter (atom 0)
            end (+ (timestamp) 1000)]
        (while (< (timestamp) end)
          (game-loop game-state)
          (swap! frame-counter inc))
        (swap! results conj @frame-counter)))
    (println "Median:" (-> @results
                           sort
                           vec
                           (get (dec (js/Math.round (/ (count @results) 2)))))
             "Mean:" (js/Math.round (/ (reduce + @results) (count @results)))
             "Max:" (-> @results sort reverse first)
             "Min:" (-> @results sort first))
    @results))


