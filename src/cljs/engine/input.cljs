(ns chocolatier.engine.input
  "Handles input from the user"
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  ;;(:require-macros [chocolatier.utils.macros :refer [defonce]])
  )


;; TODO use fan out channel to stream the input to multiple sources
;; Tracks the current state of user input
(def input-state (atom {}))

(defn debug-watcher [key state old-val new-val]
  (when (not= old-val new-val)
    (debug "State changed" key old-val "->" new-val )))

(defn keydown [e]
  (let [key (String.fromCharCode (aget e "keyCode"))]
    (swap! input-state assoc (keyword key) "on")
    (debug "Keydown:" e)))

(defn keyup [e]
  (let [key (String.fromCharCode (aget e "keyCode"))]
    (swap! input-state assoc (keyword key) "off")
    (debug "Keyup:" e)))

(defn init-keyboard!
  "Adds event listener to keyboard events. Assoc's a removal function to 
   the input-state"  
  []
  (.addEventListener js/document "keydown" keydown)
  (swap! input-state
         assoc
         :keydown
         #(.removeEventListener js/document "keydown" keydown))
  (.addEventListener js/document "keyup" keyup)
  (swap! input-state
         assoc
         :keyup
         #(.removeEventListener js/document "keyup" keyup)))

(defn start-keyboard! []
  (init-keyboard!)
  (add-watch input-state :input-debug debug-watcher))

(defn reset-keyboard! []
  (doseq [k [:keydown :keyup]]
    (let [f (k @input-state)]
      (f)))
  (remove-watch input-state :input-debug)
  (start-keyboard!))
