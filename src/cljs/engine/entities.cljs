(ns chocolatier.engine.entities
  ;; NOTE to use protocols from another ns, import them explicitely by
  ;; name and not their methods
  ;; Use them in this ns by refering to the name
  (:use [chocolatier.utils.logging :only [debug info warn error]])  
  (:require [chocolatier.engine.components :refer [Entity
                                                   Renderable
                                                   Controllable]]))


(defrecord Bunny [id sprite x y]
  Entity
  (tick [this] nil)
  
  Renderable
  (render [this stage] nil)

  Controllable
  (react-to-user-input [this state time]
    (let [sprite (:sprite this)
          input @(:input state)
          move-rate 1
          move #(condp = %
                      :W (set! (.-position.y sprite)
                               (- (.-position.y sprite) move-rate))
                      :A (set! (.-position.x sprite)
                               (- (.-position.x sprite) move-rate))
                      :S (set! (.-position.y sprite)
                               (+ (.-position.y sprite) move-rate))
                      :D (set! (.-position.x sprite)
                               (+ (.-position.x sprite) move-rate))
                      :& (set! (.-position.y sprite)
                               (- (.-position.y sprite) move-rate))
                      :% (set! (.-position.x sprite)
                               (- (.-position.x sprite) move-rate))
                      ;; Parenths are reserved so wrap it in keyword call
                      (keyword "(") (set! (.-position.y sprite)
                                            (+ (.-position.y sprite) move-rate))
                      :' (set! (.-position.x sprite)
                               (+ (.-position.x sprite) move-rate))
                      ;; Otherwise do nothing
                      nil)]
      (doseq [[k v] input]
        (when (= v "on")
          (move k))))))
