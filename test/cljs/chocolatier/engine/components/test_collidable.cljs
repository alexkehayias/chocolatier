(ns chocolatier.engine.test-collidable
  (:require-macros [cemerick.cljs.test
                    :refer [is deftest]])
  (:require [cemerick.cljs.test :refer [test-ns]]
            [chocolatier.engine.components.collidable :as c]))

(deftest test-circle-collision
  (is (true? (c/circle-collision? 50 50 10 50 50 10)))
  (is (false? (c/circle-collision? 30 30 10 50 50 10))))

(deftest test-collision
  (let [e1 {:screen-x 20 :screen-y 20
            :offset-x 4 :offset-y 4
            :height 20 :width 20
            :hit-radius 10}
        e2 {:screen-x 16 :screen-y 16
            :offset-x 4 :offset-y 4
            :height 20 :width 20
            :hit-radius 10}
        actual (c/collision? e1 e2)]
    (is (true? actual))))
