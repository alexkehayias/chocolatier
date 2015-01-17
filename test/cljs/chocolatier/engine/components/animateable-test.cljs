(ns chocolatier.components.animateable-test
  (:require-macros [cemerick.cljs.test :refer [is deftest]])
  (:require [cemerick.cljs.test :refer [test-ns]]
            [chocolatier.engine.components.animateable :as a]
            [chocolatier.engine.pixi :as pixi]))

(deftest test-get-sprite-coords
  (is (= (a/get-sprite-coords 10 10 2 2 2 1) [2 0])))

(deftest test-mk-animation-fn
  (with-redefs [pixi/set-sprite-frame! (fn [_ x y w h]
                                         (js-obj "x" x "y" y "width" w "height" h))]
    (let [result ((a/mk-animation-fn 10 10 2 2 0 0 5) (js-obj) 1)]
      (is (= (aget result "x") 2))
      (is (= (aget result "y") 0)))))

(deftest test-mk-animations-map
  (is (map? (a/mk-animations-map [:walk 10 10 2 2 0 0 5]
                                [:run 10 10 2 2 1 0 5]))))

(deftest test-mk-animation-state
  (with-redefs [pixi/mk-sprite! (fn [& args] (js-obj))
                pixi/set-sprite-frame! (fn [& args] (js-obj))]
    (let [result (a/mk-animation-state "my/image.png" 0 0 :walk
                                       [:walk 10 10 2 2 0 0 5]
                                       [:run 10 10 2 2 1 0 5])]
      (is (= #{:animation-name :sprite :animations :frame}
             (set (keys result)))))))
