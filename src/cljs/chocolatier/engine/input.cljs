(ns chocolatier.engine.input
  "Handles input from the user"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.state :as s])
  (:require-macros [chocolatier.macros :refer [defonce]]))


;; TODO use fan out channel to stream the input to multiple sources
;; Tracks the current state of user input

(defn debug-watcher [key state old-val new-val]
  (when (not= old-val new-val)
    (debug "State changed" key old-val "->" new-val )))

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
