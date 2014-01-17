(ns chocolatier.engine.entities
  ;; NOTE to use protocols from another ns, import them explicitely by
  ;; name and not their methods
  ;; Use them in this ns by refering to the name
  (:require [chocolatier.engine.components :refer [Entity Renderable
                                                   Attackable TestComp]]))


(defrecord Bunny [id sprite x y]
  Entity
  (tick [this]
    (let [sprite (:sprite this)
          old-rotation (aget sprite "rotation")]
      (aset sprite "rotation" (+ 0.01 old-rotation))))
  
  Renderable
  (render [this stage]
    (.addChild stage (:sprite this)))

  Attackable
  (attack [this] (println "owww"))

  TestComp
  (test-it [this] (println "testing")))
