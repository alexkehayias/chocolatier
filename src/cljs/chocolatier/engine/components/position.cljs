(ns chocolatier.engine.components.position)


(defrecord Position [map-x map-y screen-x screen-y screen-z])

(defn mk-position-state
  [map-x map-y screen-x screen-y screen-z]
  (->Position map-x map-y screen-x screen-y screen-z))

(defn position
  "Calculates the entities position on the map and on the screen. Listens
   for position changes in the format of [:position-change <entity-id>] with a
   message with keys for :offset-x and :offset-y"
  [entity-id component-state {:keys [inbox]}]
  ;; If there are no messages then no-op
  (if (seq inbox)
    (let [{:keys [map-x map-y screen-x screen-y screen-z]} component-state
          [offset-x offset-y] (reduce
                               (fn [[x y] {msg :msg}]
                                 [(+ x (:offset-x msg))
                                  (+ y (:offset-y msg))])
                               [0 0]
                               inbox)]
      ;; TODO translate map coords into screen coords
      (mk-position-state (- screen-x offset-x)
                         (- screen-y offset-y)
                         (- screen-x offset-x)
                         (- screen-y offset-y)
                         screen-z))
    component-state))
