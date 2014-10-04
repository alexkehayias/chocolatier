(ns chocolatier.engine.systems.input
  (:require [chocolatier.utils.logging :refer [debug]]
            [chocolatier.engine.utils.watchers :refer [hashmap-watcher]]))


(def KEYBOARD-INPUT (atom nil))

(defn keydown [e]
  (.preventDefault e)
  (let [key (String.fromCharCode (aget e "keyCode"))]
    (swap! KEYBOARD-INPUT assoc (keyword key) "on")))

(defn keyup [e]
  (.preventDefault e)
  (let [key (String.fromCharCode (aget e "keyCode"))]
    (swap! KEYBOARD-INPUT assoc (keyword key) "off")))

(defn init-input!
  "Adds event listener to input events. Assoc's a removal function to 
   the KEYBOARD-INPUT"  
  []
  ;; Add js events for keyup and keydown
  (.addEventListener js/document "keydown" keydown)
  (.addEventListener js/document "keyup" keyup)
  (add-watch KEYBOARD-INPUT :debug hashmap-watcher)
  ;; Reset the atom tracking keyboard input
  (reset! KEYBOARD-INPUT
          {:keydown #(.removeEventListener js/document "keydown" keydown)
           :keyup #(.removeEventListener js/document "keyup" keyup)
           :watchers #(remove-watch KEYBOARD-INPUT :debug)}))

(defn reset-input! []
  (debug "Resetting input")
  (when-not (empty? @KEYBOARD-INPUT)
    (debug "Removing input listeners")
    (doseq [k [:keydown :keyup :watchers]]
      (let [f (k @KEYBOARD-INPUT)]
        (f))))
  (init-input!))

(defn input-system
  "Update current user input"
  [state]
  ;; Make sure the keyboard listener is hooked up
  (when-not @KEYBOARD-INPUT (init-input!))
  (assoc-in state [:game :input] @KEYBOARD-INPUT))
