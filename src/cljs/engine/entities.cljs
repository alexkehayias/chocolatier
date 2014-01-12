(ns chocolatier.engine.entities
  (:require [chocolatier.engine.components :as c]))


(defrecord Bunny [id]
  Entity
  (tick [this] (println "ticking!"))
  
  Renderable
  (render [this] (println "rendering!!" this)))
