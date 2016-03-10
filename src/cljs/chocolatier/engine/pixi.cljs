(ns chocolatier.engine.pixi)

(defn alter-obj!
  "Alter a js object's fields. Returns the object.

   Example:
   (alter-obj! s  \"x\" 1 \"y\" 2)"
  [sprite & alterations]
  (doseq [[k v] (partition 2 alterations)]
    (aset sprite k v))
  sprite)

(defn add-child! [obj item]
  (.addChild obj item)
  obj)

(defn remove-child! [obj item]
  (.removeChild obj item)
  obj)

(defn mk-graphic!
  "Initialize a new PIXI Graphics object into the stage"
  [stage]
  (let [graphic (new js/PIXI.Graphics)]
    (add-child! stage graphic)
    graphic))

(defn mk-stage []
  (new js/PIXI.Container))

(defn mk-renderer [width height]
  (new js/PIXI.CanvasRenderer width height nil true))

(defn mk-asset-loader [image-urls assets]
  (new js/PIXI.loaders.AssetLoader (apply array assets)))

(defn mk-texture [image-location]
  (js/PIXI.Texture.fromImage image-location))

(defn set-sprite-frame!
  "Set the frame of the sprite's spritesheet coords and dimensions
   Returns the updated sprite.

   Args:
   - sprite: a Pixi Sprite obj
   - frame: a vector of pos-x, pos-y, width, height of a spritesheet"
  [sprite frame]
  (let [[x y w h] frame
        texture (.-texture sprite)
        bounds (new js/PIXI.Rectangle x y w h)]
    (set! (.-frame texture) bounds))
  sprite)

(defn mk-sprite-from-cache!
  "Returns a sprite that has been added to the stage. The image for the sprite
   is loaded from the cache. If a frame is not passed in the sprite will not
   be visible until the frame is set. See set-sprite-frame! for more info.

   Args:
   - stage: a Pixi stage object
   - loader: a Pixi.loader object
   - image-location: a path to the image to use for the sprite

   Optional args:
   - frame: a vector of x, y, w, h in relation to the sprite image"
  ([stage loader image-location]
   (mk-sprite-from-cache! stage loader image-location [0 0 0 0]))
  ([stage loader image-location frame]
   (let [cached-texture (.-texture (aget (.-resources loader) image-location))
         texture (new js/PIXI.Texture (.-baseTexture cached-texture))
         sprite (new js/PIXI.Sprite texture)]
     (set-sprite-frame! sprite frame)
     (add-child! stage sprite)
     sprite)))

(defn circle
  [graphic x y r]
  (.drawCircle graphic x y r)
  graphic)

(defn rectangle
  [graphic x y h w]
  (.beginFill graphic)
  (.drawRect graphic x y h w)
  (.endFill graphic)
  graphic)

(defn add-rect-mask!
  "Adds a mask to sprite at the screen location x, y with height and width h, w"
  [stage sprite x y h w]
  (-> (mk-graphic! stage)
      (rectangle x y h w)
      (#(alter-obj! sprite "mask" %))))

(defn mk-tiling-sprite [texture width height]
  (js/PIXI.extras.TilingSprite. texture width height))

(defn mk-display-object-container []
  (new js/PIXI.Container))

(defn mk-render-texture [w h]
  (new js/PIXI.RenderTexture w h))

(defn mk-particle-container []
  (new js/PIXI.ParticleContainer))

(defn load-assets
  "Takes an asset loader from mk-asset-loader and loads assets using the
   callback function f"
  [asset-loader f]
  (aset asset-loader "onComplete" f)
  (.load asset-loader))

(defn render!
  "Renders a pixi Stage object"
  [renderer stage]
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

(defn render-from-object-container!
  "Creates a texture from the sprites in container and renders them to the stage."
  [renderer stage container]
  (let [texture (.generateTexture container renderer)
        sprite (new js/PIXI.Sprite texture)]
    ;; This is a side-effect with no return value
    (add-child! stage sprite)))

(defn mk-text!
  "Creates a PIXI.Text object with styles.
   List of style properties here: https://pixijs.github.io/docs/PIXI.Text.html"
  [stage text styles]
  (let [text-obj (new js/PIXI.Text text styles)]
    (add-child! stage text-obj)
    text-obj))
