(ns chocolatier.engine.components.collidable)


(defn mk-collidable-state
  "Returns a hashmap of updated state with all required collision
   component state.
   - width: the width of the bounding box in pixels
   - height: the height of the bounding box in pixels
   - attributes: hashmap of attitional attributes to include in collision events"
  [width height attrs]
  {:width width
   :height height
   :attributes attrs})
