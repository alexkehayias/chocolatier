(ns chocolatier.engine.systems.input
  (:require [chocolatier.utils.logging :refer [debug]]
            [chocolatier.engine.utils.watchers :refer [hashmap-watcher]]))


(def *keyboard-input (atom nil))

(defn keydown [e]
  (.preventDefault e)
  (let [key (String.fromCharCode (aget e "keyCode"))]
    (swap! *keyboard-input assoc (keyword key) "on")))

(defn keyup [e]
  (.preventDefault e)
  (let [key (String.fromCharCode (aget e "keyCode"))]
    (swap! *keyboard-input assoc (keyword key) "off")))

(defn init-input!
  "Adds event listener to input events. Assoc's a removal function to
   the *keyboard-input"
  []
  ;; Add js events for keyup and keydown
  (.addEventListener js/document "keydown" keydown)
  (.addEventListener js/document "keyup" keyup)
  ;; (add-watch *keyboard-input :debug hashmap-watcher)
  ;; Reset the atom tracking keyboard input
  (reset! *keyboard-input
          {:keydown #(.removeEventListener js/document "keydown" keydown)
           :keyup #(.removeEventListener js/document "keyup" keyup)
           ;; :watchers #(remove-watch *keyboard-input :debug)
           }))

(defn reset-input! []
  (debug "Resetting input")
  (when (seq @*keyboard-input)
    (debug "Removing input listeners")
    (doseq [k [:keydown :keyup]]
      (let [f (k @*keyboard-input)]
        (f))))
  (init-input!))

(defn input-system
  "Update current user input"
  [state]
  ;; Make sure the keyboard listener is hooked up
  (when-not @*keyboard-input (init-input!))
  (assoc-in state [:game :input] @*keyboard-input))
