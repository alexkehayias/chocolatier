(ns chocolatier.engine.entities
  (:require [chocolatier.engine.components]))

(defrecord Bunny [id])

(extend-type Bunny
  Renderable
  (render [self] (println "rendering!!" self)))
