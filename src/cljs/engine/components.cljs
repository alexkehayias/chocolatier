(ns chocolatier.engine.components)


(defprotocol Entity
  "Base level entities can be ticked"
  (tick [this]
    "Update the world to handle the passing of a tick for this entity."))

(defprotocol Renderable
  "Render to the the screen"
  (render [this state]
    "Render this to the stage"))

(defprotocol UserInput
  "Can be controlled by user input"
  (react-to-user-input [this state time]
    "React to user input state from a keyboard or controller"))

(defprotocol Attackable
  "Render to the the screen"
  (attack [this]
    "Attack this entity"))

(defprotocol Moveable
  "Calculate changes in movement for all moveable entities"
  (move [this state]
    "Attack this entity"))

(defprotocol BackgroundLayer
  (move-layer [this state]
    "Move layer based on some game state such as the player's position"))

(defprotocol Tile
  (traversable? [this]
    "Boolean of whether this tile is traversable or not"))

(defprotocol Collidable
  (check-collision [this state time]))
