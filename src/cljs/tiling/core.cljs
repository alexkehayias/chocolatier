(ns chocolatier.tiling.core
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Tile]]))


(defn create-tile [img {:keys [x y]} ])

(defrecord background-tile [sprite x y traverse?]
  Tile
  (move-by-offset [this offset-x offset-y]
    (let [sprite (:sprite this)
          x (+ (.-position.x sprite) offset-x)
          y (+ (.-position.y sprite) offset-y)]
      (set! (.-position.x sprite) x)
      (set! (.-position.y sprite) y)))
  
  (traversable? [this] true))
