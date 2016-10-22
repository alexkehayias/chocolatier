(ns ^:figwheel-always chocolatier.devcards
    "All the devcards get registered here"
    (:require [devcards.core :as dc :refer-macros [defcard deftest dom-node]]
              [chocolatier.examples.action-rpg.core]

              ;; Tests
              [chocolatier.engine.events-test]
              [chocolatier.engine.ecs-test]
              ;; [chocolatier.components.animateable-test]

              ;; Benchmarks
              [chocolatier.engine.benchmarks]))
