(ns chocolatier.engine.entities
  (:require [chocolatier.engine.components :refer [Entity Renderable Attackable]]))


(defrecord Bunny [id sprite x y]
  Entity
  (tick [this]
    (let [sprite (:sprite this)])
    (aset sprite "rotation" (+ 0.02 (aget sprite "rotation"))))
  
  Renderable
  (render [this stage]
    (.addChild stage (:sprite this)))

  Attackable
  (attack [this] (println "owww")))
