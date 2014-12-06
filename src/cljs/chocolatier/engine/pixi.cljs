(ns chocolatier.engine.pixi)

(defn mk-graphic!
  "Initialize a new PIXI Graphics object into the stage"
  [stage]
  (let [graphic (new js/PIXI.Graphics)]
    (.addChild stage graphic)
    graphic))

(defn mk-stage []
  (new js/PIXI.Stage))

(defn mk-renderer []
  (new js/PIXI.CanvasRenderer width height nil true))

(defn mk-asset-loader [image-urls]
  (new js/PIXI.AssetLoader (apply array assets)))

(defn load-assets
  "Takes an asset loader from mk-asset-loader and loads assets using the 
   callback function f"
  [asset-loader f]
  (aset asset-loader "onComplete" f)
  (.load asset-loader))

(defn render [renderer stage]
  (.render renderer stage)) 

(defn line-style
  ([graphic weight]
   (.lineStyle graphic weight)
   graphic)
  ([graphic weight color]
   (.lineStyle graphic weight color)
   graphic))

(defn fill [graphic color-hex opacity]
  (.beginFill graphic color-hex opacity)
  graphic)

(defn clear [graphic]
  (.clear graphic)
  graphic)

(defn add-to-stage
  "Works for PIXI.Sprite and PIXI.Graphics and probably some others"
  [stage graphic]
  (.addChild stage graphic)
  graphic)

(defn circle
  [graphic x y r]
  (.drawCircle graphic x y r)
  graphic)

(defn mk-sprite! [stage image-location]
  (.addChild stage (js/PIXI.Sprite. (js/PIXI.Texture.fromImage image-location))))

(defn mk-tiling-sprite! [stage image-location width height]
  ;; TODO cache the texture instead of creating a new one all the time
  (let [texture (js/PIXI.Texture.fromImage image-location)
        sprite (js/PIXI.TilingSprite. texture width height)]
    (.addChild stage sprite)
    sprite))
