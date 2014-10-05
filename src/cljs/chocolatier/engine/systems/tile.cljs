(ns chocolatier.engine.systems.tile
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]))


(defn screen-offset
  [m x y]
  (assoc m
    :screen-x (+ (:screen-x m) x)
    :screen-y (+ (:screen-y m) y)))

(defn transpose-tiles
  [tiles offset-x offset-y]
  (map #(screen-offset % offset-x offset-y) tiles))

(defn render-tiles [this state]
  (let [{:keys [sprite screen-x screen-y]} this]
    (if (or (not= (.-position.x sprite) screen-x)
            (not= (.-position.y sprite) screen-y))
      (do
        (set! (.-position.x sprite) screen-x)
        (set! (.-position.y sprite) screen-y)
        (assoc this :sprite sprite)))))

(defn create-tile! [stage img height width traversable
                    pos-x pos-y map-x map-y]
  (let [texture (js/PIXI.Texture.fromImage img)
        sprite (new js/PIXI.TilingSprite texture height width)
        tile (new BackgroundTile sprite height width traversable
                  pos-x pos-y map-x map-y)]
    (set! (.-position.x sprite) pos-x)
    (set! (.-position.y sprite) pos-y)
    (.addChild stage sprite)
    tile))

(defn create-test-tile-map!
  "Create a test tile map of 50 x 50 tiles"
  [state stage]
  (assoc-in state [:state :tiles]
            (doall (for [x (range 0 500 50)
                         y (range 0 500 50)]
                     (create-tile! stage "static/images/tile.png" 
                                   50 50 true x y x y)))))

(defn tile-system
  "Update the tile map"
  [state]
  (let [updated-tiles (for [f fns, e entity-ids] (f state e))]
    (apply ces/deep-merge updated-tiles)))
