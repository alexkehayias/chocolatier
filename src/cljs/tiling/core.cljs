(ns chocolatier.tiling.core
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Tile Renderable
                                                   BackgroundLayer]]
            [chocolatier.engine.components :as c]
            [chocolatier.engine.state :as s]))


(defrecord TileMap [tiles x y]
  Renderable
  (render [this state]
    (info "hello from tilemap?")
    (let [updated-tiles (map #(c/render % state) (:tiles this))]
      (assoc this :tiles updated-tiles)))

  ;; Apply an offset to the tile map based on player's position
  BackgroundLayer
  (move-layer [this state]
    (let [{:keys [map-x map-y]} (first (filter :player @(:entities state)))]
      ;; TODO apply an offset 
      ;;(debug "Player position" map-x map-y)
      nil 
      )
    ))

(defrecord BackgroundTile [sprite height width x y traverse?]
  Tile
  (traversable? [this] true)
  
  Renderable
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
    tile))

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
  (let [tiles (for [x (range 0 500 50)
                    y (range 0 500 50)]
                (create-tile! stage "static/images/tile.png"
                              50 50 x y true))]
    (reset! s/tile-map (new TileMap (doall tiles) 0 0))))

