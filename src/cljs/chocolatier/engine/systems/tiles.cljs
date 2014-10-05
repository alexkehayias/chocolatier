(ns chocolatier.engine.systems.tiles
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
        sprite (new js/PIXI.TilingSprite texture height width)]
    (set! (.-position.x sprite) pos-x)
    (set! (.-position.y sprite) pos-y)
    (.addChild stage sprite)
    {:sprite sprite
     :height height
     :width width
     :traversable traversable
     :pos-x pos-x :pos-y pos-y
     :map-x map-x :map-y map-y}))

(defn create-tiles!
  "Create a test tile map of 50 x 50 tiles"
  [stage]
  (fn [state]
    (assoc-in state [:state :tiles]
              (doall (for [x (range 0 500 50)
                           y (range 0 500 50)]
                       (create-tile! stage "static/images/tile.png" 
                                     50 50 true x y x y))))))

(defn tile-system
  "Update the tile map"
  [state]
  ;; TODO do something with tiles beyond drawing them once
  (let [tiles (-> state :state :tiles)]
    (assoc-in state [:state :tiles] tiles)))
