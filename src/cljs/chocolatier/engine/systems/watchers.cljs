(ns chocolatier.engine.watchers
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [clojure.set :as set]))

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

(defn debug-watcher
  "Log any differences between two hashmaps"
  [key state old-val new-val]
  (when (not= old-val new-val)
    (debug "State changed" key (map-difference new-val old-val))))

(defn list-watcher
  "Log any differences between two lists of hashmaps"
  [key state old-val new-val]
  (when (not= old-val new-val)
    (doseq [i (range (count old-val))]
      (try (debug "State changed" key (map-difference (new-val i) (old-val i)))
           (catch js/Object e (error (str e)))))))

