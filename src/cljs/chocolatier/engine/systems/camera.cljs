(ns chocolatier.engine.systems.camera
  (:require [chocolatier.engine.events :as ev]))


(defn mk-camera-state []
  {:x 0 :y 0})

(defn camera-system
  [state]
  (let [{:keys [x y]} (get-in state [:state :camera] (mk-camera-state))
        events (get-in state (into ev/queue-path [:position-change :player1]))]
    (if (nil? events)
      state
      (let [{:keys [offset-x offset-y]} (first events)]
        (assoc-in state [:state :camera]
                  {:x (+ x offset-x)
                   :y (+ x offset-y)})))))
