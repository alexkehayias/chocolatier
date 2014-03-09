(ns chocolatier.engine.input
  "Handles input from the user"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.state :as s]
            [clojure.set :as set])
  (:require-macros [chocolatier.macros :refer [defonce]]))


(defn map-difference
  "Get the difference between hashmaps
   http://stackoverflow.com/questions/3387155/difference-between-two-maps"
  [m1 m2]
  (let [ks1 (set (keys m1))
        ks2 (set (keys m2))
        ks1-ks2 (set/difference ks1 ks2)
        ks2-ks1 (set/difference ks2 ks1)
        ks1*ks2 (set/intersection ks1 ks2)]
    (merge (select-keys m1 ks1-ks2)
           (select-keys m2 ks2-ks1)
           (select-keys m1
                        (remove (fn [k] (= (m1 k) (m2 k)))
                                ks1*ks2)))))

;; TODO use fan out channel to stream the input to multiple sources
;; Tracks the current state of user input


(defn debug-watcher [key state old-val new-val]
  ;; TODO only show the values that have changed between the old and
  ;; the new
  (when (not= old-val new-val)
    (debug "State changed" key (map-difference new-val old-val))))

(defn keydown [e]
  (.preventDefault e)
  (let [key (String.fromCharCode (aget e "keyCode"))]
    (swap! s/input assoc (keyword key) "on")))

(defn keyup [e]
  (.preventDefault e)
  (let [key (String.fromCharCode (aget e "keyCode"))]
    (swap! s/input assoc (keyword key) "off")))

(defn init-input!
  "Adds event listener to input events. Assoc's a removal function to 
   the s/input"  
  []
  (.addEventListener js/document "keydown" keydown)
  (swap! s/input
         assoc
         :keydown
         #(.removeEventListener js/document "keydown" keydown))
  (.addEventListener js/document "keyup" keyup)
  (swap! s/input
         assoc
         :keyup
         #(.removeEventListener js/document "keyup" keyup)))

(defn start-input! []
  (init-input!)
  (add-watch s/input :input-debug debug-watcher))

(defn reset-input! []
  (debug "Resetting input")
  (when-not (empty? @s/input)
    (debug "Removing input listeners")
    (doseq [k [:keydown :keyup]]
    (let [f (k @s/input)]
      (f))))  
  (remove-watch s/input :input-debug)
  (start-input!))
