(ns chocolatier.engine.components.controllable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]))


(defn include-input-state
  "State parsing function. Returns a vector of input-state-component-state
   and entity-id"
  [state component-id entity-id]
  (let [input-state (-> state :game :input)
        component-state (ces/get-component-state state component-id entity-id)]
    [input-state component-state component-id entity-id]))

(defmulti react-to-input
  (fn [input-state component-state component-id entity-id] entity-id))

(def move-rate 4)

(def keycode->direction
  {:W :n
   :S :s
   :D :e
   :A :w
   ;; Direction pad
   :& :n
   ;; Use keyword here since paranths are reserved
   (keyword "(") :s
   :' :e
   :% :w})

(def keycode->offset
  {:W [:offset-y (* 1 move-rate)]
   :S [:offset-y (* -1 move-rate)]
   :D [:offset-x (* -1 move-rate)]
   :A [:offset-x (* 1 move-rate)]
   ;; Direction pad   
   :& [:offset-y (* 1 move-rate)]
   ;; Use keyword here since parahs are reserved
   (keyword "(") [:offset-y (* -1 move-rate)]
   :' [:offset-x (* -1 move-rate)]
   :% [:offset-x (* 1 move-rate)]})

(defmethod react-to-input :player1
  [input-state component-state component-id entity-id]
  ;; (loop [offsets {:offset-x 0 :offset-y 0}
  ;;          i (seq @(:input state))]
  ;;     (let [[k v] (first i)
  ;;           remaining (rest i)
  ;;           updated-offsets (if (= v "on")
  ;;                             (apply assoc offsets (k keycode->offset))
  ;;                             offsets)]
  ;;       (if (empty? remaining)
  ;;         ;; Update the global offsets
  ;;         (swap! (:global state) assoc
  ;;                :offset-x (:offset-x updated-offsets) 
  ;;                :offset-y (:offset-y updated-offsets)) 
  ;;         (recur updated-offsets remaining))))
  (ces/mk-component-state component-state component-id entity-id))
