(ns chocolatier.engine.components)


(defprotocol Entity
  "Base level entities can be ticked"
  (tick [this]
    "Update the world to handle the passing of a tick for this entity."))

(defprotocol Renderable
  "Render to the the screen"
  (render [this stage]
    "Render this to the stage"))

(defprotocol Controllable
  "Can be controlled by user input"
  (react-to-user-input [this state time]
    "React to user input state from a keyboard or controller"))

(defprotocol Attackable
  "Render to the the screen"
  (attack [this]
    "Attack this entity"))

(defprotocol TestComp
  (test-it [this]))
