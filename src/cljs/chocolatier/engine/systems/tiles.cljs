(ns chocolatier.engine.systems.tiles
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.pixi :as pixi]))

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

(defn create-tile!
  "Adds a tile to the stage and returns the hashmap representation
   of a tile."
  [stage tileset-image
   width height 
   map-x map-y ;; coords on the grid i.e 0,1
   screen-x screen-y ;; postiion on the screen
   tileset-x tileset-y ;; position on the tileset image
   & [attrs-hm]]
  (let [sprite (pixi/mk-tiling-sprite! stage
                                       tileset-image
                                       width
                                       height)]
    (set! (.-position.x sprite) screen-x)
    (set! (.-position.y sprite) screen-y)
    (set! (.-tilePosition.x sprite) tileset-x)
    (set! (.-tilePosition.y sprite) tileset-y)
    ;; Combine the required fields with any additional attributes
    (merge {:sprite sprite
            :height height
            :width width
            :traversable traversable
            :map-x map-x :map-y map-y
            :screen-x screen-x :screen-y screen-y}
           attrs-hm)))

(defn create-tiles-from-spec!
  "Create tiles from a map-spec, a 1 dimensional array where the 
   value of the item represents it's location in the tile set and 
   the index of the item represents it's location in the tile map.

   Example of a 4 by 4 map spec:
   [0 1 2 0
    0 1 1 1
    1 1 2 0 
    0 1 1 0]

    Args:
    - map-w: number of tiles wide
    - map-h: number of tiles high
    - tile-h: pixel height of a tile
    - tile-w: pixel width of a tile
    - tileset-h: number of tiles high in the tileset
    - tileset-w: number of tiles wide in the tileset
    - map-spec: a one dimensional array of integers"
  [stage tileset-image
   map-w map-h
   tileset-w tileset-h
   tile-px-w tile-px-h
   map-spec
   & {:keys [offset-x offset-y]
      :or {offset-x 0 offset-y 0}}]
  (doall
   (for [[indx tile-pos] (map-indexed vector map-spec)
         ;; Only draw tiles that have a tileset location
         ;; Tiled specifies that 0 is no tile
         :when (> tile-pos 0)]
     ;; TODO put this in a separate function for testing
     (let [map-row (js/Math.floor (/ indx map-w))
           map-col (if (> (inc indx) map-w)
                     (- indx (* map-row map-w))
                     indx)
           map-x (* map-col tile-px-w)
           map-y (* map-row tile-px-h)

           ;; Tile positions are indexed to 1
           tileset-pos (dec tile-pos)
           tileset-row (js/Math.floor (/ tileset-pos tileset-w))
           tileset-col (if (> (inc tileset-pos) tileset-w)
                         (- tileset-pos (* tileset-row tileset-w))
                         tileset-pos)
           ;; Need to take the inverse of the coordinance since
           ;; setting the tileset image x y is right aligned
           tileset-x (- (* tileset-w tile-px-w) (* tileset-col tile-px-w))
           tileset-y (- (* tileset-h tile-px-h) (* tileset-row tile-px-h))]
       ;;[map-row map-col map-x map-y tileset-row tileset-col
       ;;tileset-x tileset-y]
       (println :tile-pos tile-pos
                :tileset-w tileset-w
                :tileset-h tileset-h
                :tileset-pos tileset-pos
                :tileset-row tileset-row
                :tileset-col tileset-col
                :tileset-x tileset-x
                :tileset-y tileset-y)
       (create-tile! stage tileset-image
                     tile-px-w tile-px-h 
                     map-row map-col
                     map-x map-y
                     tileset-x tileset-y)))))

(defn mk-tiles-from-tilemap!
  "Returns a collection of tile hashmaps according to the spec.
   Args:
   - spec: a json encoded map exported from Tiled see
     http://www.mapeditor.org/"
  [stage tilemap]
  (fn [state]
    (let [{:keys [width
                  height
                  layers
                  tilesets
                  tileheight
                  tilewidth]} tilemap
                  {:keys [imageheight imagewidth]} (-> tilesets first)
           tileset-width (/ imagewidth tilewidth) 
           tileset-height (/ imageheight tileheight)]
      ;; Draw tiles from all layers of the tile map
      (assoc-in state [:state :tiles]
                (doall
                 (for [l layers]
                   (create-tiles-from-spec! stage (-> tilesets first :image)
                                            width height
                                            tileset-width tileset-height
                                            tilewidth tileheight
                                            (:data l))))))))

(defn load-tilemap
  "Async load a json tilemap at the url. Calls callback function with the
   tilemap as a hashmap"
  [url callback]
  (.send goog.net.XhrIo url
         #(callback (js->clj (.getResponseJson (.-target %))
                             :keywordize-keys true))))

(defn tile-system
  "Update the tile map"
  [state]
  ;; TODO do something with tiles beyond drawing them once
  (let [tiles (-> state :state :tiles)]
    (assoc-in state [:state :tiles] tiles)))
