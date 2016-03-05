(ns chocolatier.engine.systems.input
  (:require [chocolatier.utils.logging :refer [debug]]
            [chocolatier.engine.ecs :as ecs]
            [chocolatier.engine.utils.watchers :refer [hashmap-watcher]]))


(def *keyboard-input (atom nil))

(defn keydown [e]
  (.preventDefault e)
  (let [key (String.fromCharCode (aget e "keyCode"))]
    (swap! *keyboard-input assoc (keyword key) :on)))

(defn keyup [e]
  (.preventDefault e)
  (let [key (String.fromCharCode (aget e "keyCode"))]
    (swap! *keyboard-input dissoc (keyword key))))

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

;; TODO refactor this to emit events
(defn keyboard-input-system
  "Adds keyboard input component state to any entity that has the
   :keyboard-input component"
  [state]
  ;; Make sure the keyboard listener is hooked up, if not start it
  (let [in @*keyboard-input
        entity-ids (ecs/entities-with-component state :keyboard-input)]
    (if in
      (assoc-in state [:state :keyboard-input]
                (into {} (map (fn [e] [e in])) entity-ids))
      (do (init-input!) state))))
