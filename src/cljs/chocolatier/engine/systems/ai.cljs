(ns chocolatier.engine.systems.ai
  "System for handling entity artificial intelligence"
  (:require [chocolatier.engine.ces :as ces]))


(defn ai-system
  [state fns entity-ids]
  (ces/iter-fns state (for [f fns, e entity-ids] #(f % e))))
