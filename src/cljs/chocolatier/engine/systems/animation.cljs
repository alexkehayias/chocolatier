(ns chocolatier.engine.systems.animation)

;; Maybe return a function that given a sprite and a frame number
;; changes the sprite viewport correctly

(defn animation-system
  [state f entity-ids]
  (reduce f state entity-ids))
