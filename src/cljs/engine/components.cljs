(ns chocolatier.engine.components)

(defprotocol Renderable
  "Render to the the screen"
  (render [_]))
