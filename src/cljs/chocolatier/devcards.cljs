(ns ^:figwheel-always chocolatier.devcards
    "All the devcards get registered here"
    (:require [devcards.core :as dc :refer-macros [defcard deftest dom-node]]
              [chocolatier.examples.action-rpg.core]

              ;; Tests
              [chocolatier.engine.systems.events-test]
              [chocolatier.engine.ces-test]
              [chocolatier.components.animateable-test]

              ;; Benchmarks
              [chocolatier.engine.benchmarks]

              [chocolatier.engine.ces :as ces]
              [chocolatier.engine.core :refer [mk-game-state
                                               request-animation
                                               game-loop]]
              [chocolatier.engine.systems.events :refer [event-system
                                                         init-events-system]]))
