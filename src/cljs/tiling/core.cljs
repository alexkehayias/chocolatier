(ns chocolatier.tiling.core
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Tile Renderable]]
            [chocolatier.engine.state :as s]))



;; TODO create a TileMap record that takes a list of tiles and can
;; transform all of them without a loop over each tile

(defrecord BackgroundTile [sprite height width x y traverse?]
  Tile
  (traversable? [this] true)
  
  Renderable
  ;; Render sets the x y position of the tile on the screen only if
  ;; the tile position has changed
  (render [this state]
    (let [sprite (:sprite this)]
      (if (or (not= (.-position.x sprite) (:x this))
              (not= (.-position.y sprite) (:y this)))
        (do
          (debug "Moving tiles!")
          (set! (.-position.x sprite) (:x this))
          (set! (.-position.y sprite) (:y this))
          (assoc this :sprite sprite))
        this))))

(defn create-tile! [stage img height width x y traversable]
  ;; (debug "Creating tile" stage img height width x y traversable)
  (let [texture (js/PIXI.Texture.fromImage img)
        sprite (new js/PIXI.TilingSprite texture height width)
        tile (new BackgroundTile sprite height width x y traversable)]
    ;; Initialize the position on the screen
    (set! (.-position.x sprite) x)
    (set! (.-position.y sprite) y)
    (.addChild stage sprite)
    (swap! s/tiles conj tile)))

(defn load-tile-map!
  "Create a tile map from a hash-map spec.

   {
     :tiles [
       {:width 50 :height 50 :x 0 :y 0 
        :traversable? true 
        :img \"static/images/tile.png\"}
    ]

   }
  "
  [stage map-spec]
  (doseq [{:keys [height width x y traversable? img]} (:tiles map-spec)]
    (create-tile! stage img height width x y traversable?)))

(defn load-test-tile-map!
  "Create a test tile map of 50 x 50 tiles"
  [stage]
  (doseq [x (range 0 500 50)
          y (range 0 500 50)]
    (create-tile! stage "static/images/tile.png" 50 50 x y true)))

