(ns ^:figwheel-always chocolatier.devcards
    (:require [devcards.core :as dc :refer-macros [defcard deftest dom-node]]
              [dommy.core :as dom :refer-macros [sel sel1]]
              [chocolatier.engine.ces :as ces]
              [chocolatier.engine.core :refer [mk-game-state
                                               request-animation
                                               game-loop]]
              [chocolatier.engine.systems.events :refer [event-system
                                                         init-events-system]]))


(defn simple-game
  "Starts a game loop"
  [running? node width height & specs]
  (let [stage (new js/PIXI.Container)
        options (clj->js {"transparent" true})
        renderer (new js/PIXI.CanvasRenderer width height options)
        default-specs [[:renderer renderer stage]]
        state (apply mk-game-state {} (concat default-specs specs))]
    (dom/append! node (.-view renderer))
    ;; Start the game loop
    (request-animation #(game-loop state running?))))

(defonce running? (atom true))

;; TODO replace all of the code needed to handle teardown into a
;; function/macro so all we need is to have collection of specs
(defcard "# This is an example game loop."
  (dom-node
   (fn [rendered? node]
     (let [start-game! #(do (reset! running? true)
                            (simple-game
                             running?
                             node
                             200
                             200
                             [:scene :default [:s1]]
                             [:current-scene :default]
                             [:system :s1 (fn [state f entity-ids]
                                            (reduce f state entity-ids))
                              :c1]
                             [:component :c1 (fn [entity-id component-state context]
                                               (update component-state :counter inc))]
                             [:entity :p1 [:c1]]))]
       ;; Since this involves a game loop we need to know that it is
       ;; already rendered so we can property tear down the game loop
       (if @rendered?
         ;; Remove the children of the node and start the game
         (do (reset! running? false)
             (dom/clear! node)
             ;; Give it a second to end the game
             (js/setTimeout start-game! 1000))
         (reset! rendered? true)))))
  (atom false))
