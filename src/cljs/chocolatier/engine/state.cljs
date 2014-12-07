(ns chocolatier.engine.state
  (:require [chocolatier.engine.utils.watchers :refer [hashmap-watcher
                                                       entity-watcher]])
  (:require-macros [chocolatier.macros :refer [defonce]]))


;; TODO a function to inspect/capture all of the current state that
;; can be read in to a game to start it

;; Global game state
(defonce global (atom {:offset-x 0 :offset-y 0}))
(defonce game (atom nil))
(defonce input (atom {}))
;; TODO Should entities be a hashmap for easy removal from the game
;; state without having to iterate through the whole list? Or will we
;; need to be filtering the whole list any way
(defonce entities (atom {}))
(defonce systems (atom {}))
(defonce tile-map (atom {}))

;; Graphics object is mutated and stored here
(def hit-zones (atom nil))

(def state
  {:game game
   :global global
   :systems systems
   :input input
   :entities entities
   :tile-map tile-map
   :hit-zones hit-zones})

;; Watchers
(defn add-watches! []
  (add-watch global :global hashmap-watcher)
  (add-watch input :input hashmap-watcher)
  (add-watch entities :entities entity-watcher))

(defn remove-watches! []
  (remove-watch global :global)
  (remove-watch input :input)
  (remove-watch entities :entities))

(defn reset-state! []
  (remove-watches!)
  (reset! global {:offset-x 0 :offset-y 0})
  (reset! game nil)
  (reset! entities {})
  (reset! input {})
  (reset! systems {})
  (reset! tile-map {})
  (reset! hit-zones nil)
  (add-watches!))
