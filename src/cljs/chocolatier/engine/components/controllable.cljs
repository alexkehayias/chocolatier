(ns chocolatier.engine.components.controllable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]))


(defn include-input-state
  "State parsing function. Returns a vector of input-state, component-state
   component-id and entity-id"
  [state component-id entity-id]
  (let [input-state (-> state :game :input)
        component-state (ces/get-component-state state :renderable entity-id)]
    [input-state component-state component-id entity-id]))

;; TODO remove!!! This alters another component's state!
;; Replace with an event emitter
(defn output-to-renderable
  "Format function that outputs result of a component function into component
   state of :renderable and returns updated game state"
  [state component-id entity-id result]
  (ces/mk-component-state state :renderable entity-id result))

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

(defn get-offsets
  "Returns offset-x and offset-y based on input state as a hashmap"
  [input-state]
  (loop [offsets {:offset-x 0 :offset-y 0}
         i (seq input-state)]
    (let [[k v] (first i)
          remaining (rest i)
          updated-offsets (if (= v "on")
                            (apply assoc offsets (k keycode->offset))
                            offsets)]
      (if (empty? remaining)
        offsets 
        (recur updated-offsets remaining)))))

(defmethod react-to-input :default
  [input-state component-state component-id entity-id]
  component-state)

(defmethod react-to-input :player1
  [input-state component-state component-id entity-id]
  (merge component-state (get-offsets input-state)))


