(ns chocolatier.components.animateable-test
  (:require [cljs.test :refer-macros [is testing run-tests]]
            [devcards.core :as dc :refer-macros [defcard deftest dom-node]]
            [chocolatier.engine.components.animateable :as a]
            [chocolatier.engine.pixi :as pixi]))


(defcard "# Animation Component Tests")

(deftest test-get-sprite-coords
  (is (= (a/get-sprite-coords 10 10 2 2 2 1) [2 0])))

(deftest test-mk-animation-fn
  "Test that when we call an animation function returned by mk-animation-fn with
  a frame number we get the expected spritesheet position"
  (let [[x y frame-w frame-h] ((a/mk-animation-fn 10 10 2 2 0 0 5) 1)]
    (is (= x 2))
    (is (= y 0))
    (is (= frame-w 2))
    (is (= frame-h 2))))

(deftest test-mk-animations-map
  (is (map? (a/mk-animations-map [:walk 10 10 2 2 0 0 5]
                                 [:run 10 10 2 2 1 0 5]))))

(deftest test-mk-animateable-state
  (let [result (a/mk-animateable-state :walk
                                       [:walk 10 10 2 2 0 0 5]
                                       [:run 10 10 2 2 1 0 5])]
    (is (= #{:animation-stack :animations :frame}
           (set (keys result))))))
