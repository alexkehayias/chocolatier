(ns chocolatier.engine.pixi)

(defn add-child! [obj item]
  (.addChild obj item)
  obj)

(defn mk-graphic!
  "Initialize a new PIXI Graphics object into the stage"
  [stage]
  (let [graphic (new js/PIXI.Graphics)]
    (add-child! stage graphic)
    graphic))

(defn mk-stage []
  (new js/PIXI.Stage))

(defn mk-renderer [width height]
  (new js/PIXI.CanvasRenderer width height nil true))

(defn mk-asset-loader [image-urls assets]
  (new js/PIXI.AssetLoader (apply array assets)))

(defn mk-texture [image-location]
  (js/PIXI.Texture.fromImage image-location))

(defn mk-sprite! [stage image-location]
  (add-child! stage (js/PIXI.Sprite. (js/PIXI.Texture.fromImage image-location))))

(defn mk-tiling-sprite [texture width height]
  (js/PIXI.TilingSprite. texture width height))

(defn mk-display-object-container []
  (new js/PIXI.DisplayObjectContainer))

(defn mk-render-texture [w h]
  (new js/PIXI.RenderTexture w h))

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
  (add-child! stage graphic)
  graphic)

(defn circle
  [graphic x y r]
  (.drawCircle graphic x y r)
  graphic)

(defn render-from-object-container
  "Creates a texture from the sprites in container and renders them to the stage."
  [stage container w h]
  (let [texture (mk-render-texture w h)]
    ;; This is a side-effect with no return value
    (.render texture container)
    (add-child! stage (new js/PIXI.Sprite texture))))
