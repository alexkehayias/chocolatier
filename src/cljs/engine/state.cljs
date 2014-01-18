(ns chocolatier.engine.state
  (:require-macros [chocolatier.macros :refer [defonce]]))


;; TODO a function to inspect/capture all of the current state that
;; can be read in to a game to start it

;; Global game state
(defonce game (atom nil))

;; TODO Should entities be a hashmap for easy removal from the game
;; state without having to iterate through the whole list? Or will we
;; need to be filtering the whole list any way
(defonce entities (atom []))

(defonce systems (atom {}))

(def state
  {:game game
   :systems systems
   :entities entities})

(defn reset-state! []
  (reset! game nil)
  (reset! entities [])
  (reset! systems {}))
