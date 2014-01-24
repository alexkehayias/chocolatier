(ns chocolatier.tiling.core
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Tile]]
            [chocolatier.engine.state :as s]))


(defrecord BackgroundTile [sprite height width x y traverse?]
  Tile
  (move-by-offset [this offset-x offset-y]
    (let [sprite (:sprite this)
          x (+ (.-position.x sprite) offset-x)
          y (+ (.-position.y sprite) offset-y)]
      (set! (.-position.x sprite) x)
      (set! (.-position.y sprite) y)))
  
  (traversable? [this] true))

(defn create-tile [stage img height width x y traversable]
  (let [texture (js/PIXI.Texture.fromImage img)
        sprite (new js/PIXI.TilingSprite texture height width)
        tile (new BackgroundTile sprite height width x y traversable)]            
    (set! (.-position.x sprite) x)
    (set! (.-position.y sprite) y)
    (.addChild stage sprite)
    (swap! s/tile-map conj tile)))
