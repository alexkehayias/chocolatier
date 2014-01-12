(ns chocolatier.engine.entities
  (:require [chocolatier.engine.components :as c]))


(defrecord Bunny [id]
  c/Entity
  (c/tick [this] (println "ticking!"))
  
  c/Renderable
  (c/render [this] (println "rendering!!" this)))
