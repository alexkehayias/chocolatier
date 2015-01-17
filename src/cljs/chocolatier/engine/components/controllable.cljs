(ns chocolatier.engine.components.controllable
  (:require [chocolatier.utils.logging :as log]
            [chocolatier.engine.ces :as ces]
            [chocolatier.engine.systems.events :as ev]))


(defn include-input-state
  "State parsing function. Returns a vector of input-state, component-state
   component-id and entity-id"
  [state component-id entity-id]
  (let [input-state (-> state :game :input)
        component-state (ces/get-component-state state component-id entity-id)
        inbox (ev/get-subscribed-events state entity-id)]
    [input-state component-state component-id entity-id inbox]))

(def move-rate 4)

(def keycode->direction
  {:W :walk-up    ;; :n
   :S :walk-down  ;; :s
   :D :walk-right ;; :e
   :A :walk-left  ;; :w
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
   (keyword \() [:offset-y (* -1 move-rate)]
   :' [:offset-x (* -1 move-rate)]
   :% [:offset-x (* 1 move-rate)]})

(defn get-offsets
  "Returns offset-x and offset-y based on input state as a hashmap"
  [input-state]
  (loop [offsets {:offset-x 0 :offset-y 0}
         input-seq (seq input-state)]
    (if (empty? input-seq)
      offsets
      (let [[k v] (first input-seq)
            offsets (if (and (= v "on") (k keycode->offset))
                      (into offsets [(k keycode->offset)
                                     ;; WARNING: There can only be one
                                     ;; direction, last input wins
                                     [:direction (k keycode->direction)]])
                      offsets)]
        (recur offsets (rest input-seq))))))

(defn reverse-offsets [{:keys [offset-x offset-y] :as offset-hm}]
  {:offset-x (if (pos? offset-x) (- offset-x) (max offset-x (- offset-x)))
   :offset-y (if (pos? offset-y) (- offset-y) (max offset-y (- offset-y)))})

(defmulti react-to-input
  (fn [input-state component-state component-id entity-id inbox] entity-id))

(defmethod react-to-input :default
  [input-state component-state component-id entity-id inbox]
  component-state)

(defmethod react-to-input :player1
  [input-state component-state component-id entity-id inbox]
  (let [offsets (get-offsets input-state)
        move-change? (or (not= (:offset-x offsets) 0)
                         (not= (:offset-y offsets) 0))
        replay? (= (:B input-state) "on")
        events []
        events (if move-change?
                 (concat events [(ev/mk-event offsets :move-change entity-id)
                                 (ev/mk-event {:action (:direction offsets)} :action entity-id)])
                 events)
        events (if replay?
                 (conj events (ev/mk-event {:replay? true} :replay))
                 events)]
    (if (empty? events)
      offsets
      [offsets events])))
