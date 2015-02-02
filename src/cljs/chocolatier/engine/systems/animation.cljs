(ns chocolatier.engine.systems.animation
  (:require [chocolatier.engine.ces :as ces]))

;; Maybe return a function that given a sprite and a frame number
;; changes the sprite viewport correctly

(defn animation-system
  [state fns entity-ids]
  (ces/iter-entities state fns entity-ids))


