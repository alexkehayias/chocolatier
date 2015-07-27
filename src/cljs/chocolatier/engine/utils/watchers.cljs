(ns chocolatier.engine.utils.watchers
  (:use [chocolatier.utils.logging :only [debug info warn error]])
  (:require [clojure.set :as set]))


(defn record->hashmap [r]
  (into {} (seq r)))

(defn map-difference
  "Get the difference between hashmaps
   http://stackoverflow.com/questions/3387155/difference-between-two-maps

   m1 is the new val m2 is the old val"
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

(defn diff->str
  "Stringify a diff by showing the old vs new val.
   Returns a string with a newline for each diff value.

   Example:
       :screen-x 240 -> 245
       :offset-x 0 -> 5"
  [diff original-vals]
  (let [lines (for [[k new-val] diff]
                ;; Format the string like :key 1 -> 2
                (str "    " k " " (k original-vals) " -> " new-val))]
                   ;; Add in a preceding newline with no space after
    (str "\n" (reduce #(str %1 "\n" %2) lines))))

(defn hashmap-watcher
  "Log any differences between two hashmaps"
  [key state old-val new-val]
  (when (not= old-val new-val)
    (let [diff (map-difference new-val old-val)]
      (when (seq diff)
        (debug "State changed" key (diff->str diff old-val))))))

(defn seq-watcher
  "Log any differences between two seqs if new items were added to the seq"
  [key state old-val new-val]
  (when (and (not= old-val new-val) (> (count new-val) (count old-val)))
    (debug "State changed" key "\nItems added:"
             ;; Slice the difference in length of the events
             (subvec new-val (count old-val)))))
