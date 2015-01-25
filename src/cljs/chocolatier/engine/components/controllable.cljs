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
  {:W :up
   :S :down
   :A :left   
   :D :right
   ;; Direction keys
   :& :up
   ;; Use keyword here since parenths are reserved
   (keyword "(") :down
   :' :right
   :% :left})

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

(defn react-to-input
  [input-state component-state component-id entity-id inbox]
  (let [{:keys [direction offset-x offset-y] :as offsets} (get-offsets input-state)
        move-change? (or (not= offset-x 0) (not= offset-y 0))
        [action direction] (if move-change?
                             [:walk direction]
                             ;; Default to :down so we don't need to
                             ;; initialize any state for this component
                             [:stand (or (:direction component-state) :down)])
        replay? (= (:B input-state) "on")
        events (if move-change?
                 ;; TODO allow different actions like
                 ;; run/walk/attack/etc by some key mapping
                 [(ev/mk-event offsets :move-change entity-id)
                  (ev/mk-event {:action action :direction direction}
                               :action entity-id)]
                 ;; If we are not moving we are standing, take the
                 ;; last direction from the component state of last
                 ;; frame
                 [(ev/mk-event {:action action :direction direction}
                                :action entity-id)])
        events (if replay?
                 (conj events (ev/mk-event {:replay? true} :replay))
                 events)
        updated-state (assoc component-state :action action :direction direction)]
    (if (empty? events)
      updated-state
      [updated-state events])))
