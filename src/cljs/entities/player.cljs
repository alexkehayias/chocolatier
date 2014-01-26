(ns chocolatier.entities.player
  ;; NOTE to use protocols from another ns, import them explicitely by
  ;; name and not their methods
  ;; Use them in this ns by refering to the name
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [chocolatier.engine.components :refer [Entity
                                                   Renderable
                                                   UserInput]]))


(defrecord Player [id sprite x y]
  Entity
  (tick [this] this)
  
  Renderable
  (render [this stage]
    (let [sprite (:sprite this)]
      ;; TODO check the current vs the new and only set if it's new
      (set! (.-position.x sprite) (:x this))
      (set! (.-position.y sprite) (:y this))
      (assoc this :sprite sprite)))

  UserInput
  ;; TODO update the player's velocity, direction
  ;; Maybe move this to collision detection system
  ;; Calculate given their velocity if they will be colliding with
  ;; something
  ;;(assoc this :x (+ 1 (:x this)))
  (react-to-user-input [this state time]
    (let [sprite (:sprite this)
          input @(:input state)
          move-rate 0.25          
          move #(condp = %2
                  :W (assoc %1 :y (- (:y %1) move-rate))
                  :A (assoc %1 :x (- (:x %1) move-rate))
                  :S (assoc %1 :y (+ (:y %1) move-rate))
                  :D (assoc %1 :x (+ (:x %1) move-rate))
                  ;; :& (set! (.-position.y sprite)
                  ;;          (- (.-position.y sprite) move-rate))
                  ;; :% (set! (.-position.x sprite)
                  ;;          (- (.-position.x sprite) move-rate))
                  ;; ;; Parenths are reserved so
                  ;; ;; wrap it in keyword call
                  ;; (keyword "(") (set! (.-position.y sprite)
                  ;;                     (+ (.-position.y sprite) move-rate))
                  ;; :' (set! (.-position.x sprite)
                  ;;          (+ (.-position.x sprite) move-rate))
                  ;; Otherwise do nothing
                  %1)]
      ;; Apply all the changes to the record in a recursive loop
      (loop [out this
             i (seq input)]
        (let [[k v] (first i)
              remaining (rest i)
              updated (if (= v "on") (move out k) out)]
          (if (empty? remaining)
            updated
            (recur updated remaining)))))))
