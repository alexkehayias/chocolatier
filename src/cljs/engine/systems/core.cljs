(ns chocolatier.engine.systems.core
  "Describes the various systems for controlling the game logic"
  (:use [chocolatier.utils.logging :only [debug info warn error]]
        [chocolatier.engine.systems.render :only [render-system]]
        [chocolatier.engine.systems.tick :only [tick-system]]
        [chocolatier.engine.systems.input :only [input-system]]
        [chocolatier.engine.systems.tile :only [tile-system]])
  (:require [chocolatier.engine.state :as s])
  (:require-macros [chocolatier.macros :refer [defonce]]))


(defn register-system!
  "Register a system to the game. Key k must be a unique.
   Overrides a system if key already exists"
  [k f]
  (when (contains? @s/systems k)
    (warn "System" k "already exists. Overriding."))
  (swap! s/systems assoc k f))


(defn init-systems! []
  (let [systems [[:tick tick-system]
                 [:input input-system]
                 [:tile tile-system]]]
    (doseq [[name system] systems]
      (register-system! name system))))

;; TODO add in all the core systems
;; TODO How do we allow the order and list of systems to be set?
;; Some sort of editable sort ordering based on system name?
;; Built in systems can have reserved system names

;; Defining a system
;; A system is called on every step through the game loop
;; It must provide a function that takes the game state as
;; an argument

;; TODO macro defsystem defines a system and adds it to systems

;; Example:
;; (defn render-system [state]
;;   (doseq [entity @(:entities state)]
;;     (when (satisfies? Renderable entity)
;;       (render entity))))

;; (defonce register-render-system
;;   (register-system! render-system))

;; System example:
;; Render
;; (when (satisfies? obj renderable)
;;       (draw obj))
