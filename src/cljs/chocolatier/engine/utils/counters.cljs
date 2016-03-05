(ns chocolatier.engine.utils.counters)


(defn mk-cooldown [limit]
  {:counter 0 :limit limit})

(defn ^boolean cooldown?
  "Takes a cooldown hashmap and returns a boolean if it is in cooldown"
  [{:keys [counter limit]}]
  (cond
    (zero? counter) false
    (> counter limit) true
    :else true))

(defn tick-cooldown
  "Increments counter of the cooldown and returns a pair of updated
   cooldown and boolean of whether it's still in cooldown. Resets
   counter to 0 if cooldown counter exceeds the limit. Cooldown is not
   in progress only if the counter is 0."
  [{:keys [counter limit] :as cooldown}]
  (cond
    (zero? counter) [(update cooldown :counter inc) false]
    (> (inc counter) limit) [(assoc cooldown :counter 0) true]
    :else [(update cooldown :counter inc) true]))
