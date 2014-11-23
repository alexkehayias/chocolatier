(ns chocolatier.engine.systems.collision
  "System for rendering entities"
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]))


(defn collision-system
  [state fns entity-ids]
  (let [entity-ids (ces/entities-with-component (:entities state) :collidable)
        entities (map #(assoc (ces/get-component-state state :renderable %) :id %)
                      entity-ids)
        ;; Sort entities by x and y
        entities-x (sort-by :x entities)
        entities-y (sort-by :y entities)]
    (ces/iter-fns state (for [f fns, e entity-ids]
                          #(f % e :entities-x entities-x :entities-y entities-y)))))
