(ns chocolatier.engine.tiling
  (:require [chocolatier.utils.logging :as log]))


(defn offset
  [m x y]
  (assoc m
    :screen-x (+ (:screen-x m) x)
    :screen-y (+ (:screen-y m) y)))

(defn transpose-tiles
  [tiles state]
  (let [{:keys [offset-x offset-y]} @(:global state)
        tiles (:tiles this)
        updated-tiles (map #(offset % offset-x offset-y) tiles)]
    (assoc this :tiles updated-tiles)))

(defn render-tiles [this state]
  (let [{:keys [sprite screen-x screen-y]} this]
    (if (or (not= (.-position.x sprite) screen-x)
            (not= (.-position.y sprite) screen-y))
      (do
        (set! (.-position.x sprite) screen-x)
        (set! (.-position.y sprite) screen-y)
        (assoc this :sprite sprite))
      this)))

(defn create-tile! [stage img height width traversable
                    screen-x screen-y
                    map-x map-y]
  (let [texture (js/PIXI.Texture.fromImage img)
        sprite (new js/PIXI.TilingSprite texture height width)
        tile (new BackgroundTile sprite height width traversable
                  screen-x screen-y map-x map-y)]
    (set! (.-position.x sprite) screen-x)
    (set! (.-position.y sprite) screen-y)
    (.addChild stage sprite)
    tile))

(defn load-test-tile-map!
  "Create a test tile map of 50 x 50 tiles"
  [stage]
  (doall
   (for [x (range 0 500 50)
         y (range 0 500 50)]
     (create-tile! stage "static/images/tile.png" 
                   50 50 true x y x y))) )

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
  (doall
   (for [{:keys [img height width traversable?
                 screen-x screen-y
                 map-x map-y]} (:tiles map-spec)]
     (create-tile! stage img height width traversable?
                   screen-x screen-y
                   map-x map-y))))
