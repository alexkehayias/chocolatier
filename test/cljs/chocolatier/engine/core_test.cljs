(ns chocolatier.engine.core-test
  (:require [devcards.core :as dc :refer-macros [defcard deftest dom-node]]
            [cljs.test :refer-macros [is testing run-tests]]
            [chocolatier.engine.core :as core]))


(defcard "# Engine Core Tests")

(deftest test-mk-state
  (testing "Test using mk-state to construct game state"
    (assert false)))
