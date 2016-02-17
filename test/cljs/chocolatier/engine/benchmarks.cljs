(ns chocolatier.engine.benchmarks
  "Basic benchmarks for measuring frames per second for the engine"
  (:require [cljs.test :refer-macros [is testing run-tests]]
            [devcards.core :as dc :refer-macros [defcard deftest dom-node]]
            [chocolatier.utils.devcards :refer [str->markdown-code-block]]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.core :refer [mk-game-state
                                             next-state
                                             timestamp]]
            [chocolatier.engine.systems.events :refer [mk-event]]))


(defcard "# Benchmarks
The benchmarks below test the performance of the game engine internals.
Real world results are likely to be different and these currently don't test
the performance of rendering to canvas/webgl.")

(defn system-fn
  [state f entity-ids]
  (reduce f state entity-ids))

(defn component-fn-state-change-only
  "Only updates the component state, does not emit any events"
  [eid cmp-state events]
  (update-in cmp-state [:x] inc))

(defn component-fn-state-change-and-events
  "Updates the component state and emits events"
  [eid cmp-state events]
  [(update-in cmp-state [:x] inc)
   [(mk-event {:testing "hi"} [eid :ev1])]])

(def simple
  "Returns game state for a simple single system, single component, single entity"
  (mk-game-state {}
                 [:scene :default [:s1]]
                 [:current-scene :default]
                 [:system :s1 system-fn :c1]
                 [:component :c1 component-fn-state-change-only]
                 [:entity :e1 [:c1]]))

(def many-entities
  "Returns game state for a simple single system, single component, single entity"
  (let [entities (doall
                  (map #(vector :entity (keyword (str "e" %)) [:c1])
                         (range 1000)))
        args (concat [{}
                      [:scene :default [:s1]]
                      [:current-scene :default]
                      [:component :c1 component-fn-state-change-only]
                      [:system :s1 system-fn :c1]]
                     entities)]
    (apply mk-game-state args)))

(def many-systems
  "Returns a game state with 100 entities 100 components and 100 systems"
  (let [entities (doall
                  (map #(vector :entity (keyword (str "e" %))
                                (doall (map (fn [n] (keyword (str "c" n)))
                                            (range 100))))
                       (range 100)))
        systems (doall
                 (map #(vector :system (keyword (str "s" %))
                               system-fn (keyword (str "c" %)))
                      (range 100)))
        components (doall
                    (map #(vector :component (keyword (str "c" %))
                                  component-fn-state-change-only)
                         (range 100)))
        scene [:scene :default (doall (map #(keyword (str "s" %)) (range 100)))]
        args (concat [{}
                      scene
                      [:current-scene :default]]
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
          (next-state game-state)
          (swap! frame-counter inc))
        (swap! results conj @frame-counter)))
    {:median (-> @results
                 sort
                 vec
                 (get (dec (js/Math.round (/ (count @results) 2)))))
     :mean (js/Math.round (/ (reduce + @results) (count @results)))
     :max (-> @results sort reverse first)
     :min (-> @results sort first)
     :run-count runs
     :run-results @results}))

(defcard "## Simple Game Loop Benchmark
A baseline test for a simple game loop.
"
  (str->markdown-code-block
   (with-out-str (clojure.repl/source simple))))

(defcard "### Results
Number of times the game loop runs per second
"
  (run-benchmark simple 5))

(defcard "## Many Entities Benchmark
What happens when there are many entities in the game state,
but minimal functionality?"
  (str->markdown-code-block
   (with-out-str (clojure.repl/source many-entities))))

(defcard "### Results
Number of times the game loop runs per second
"
  (run-benchmark many-entities 5))

(defcard "## Many Systems Benchmark
What happens when there are 100 systems and 100 entities?
"
  (str->markdown-code-block
   (with-out-str (clojure.repl/source many-systems))))

(defcard "### Results
Number of times the game loop runs per second
"
  (run-benchmark many-systems 5))
