(ns chocolatier.engine.state)

;; TODO implement a mutable js-obj that participates in the
;; Associative protocols

(deftype GameState []
  Object
  (toString [coll]
    (pr-str* coll))

  (get [coll k not-found]
    (-lookup coll k not-found))

  ILookup
  (-lookup [coll k]
    (-lookup coll k nil))

  (-lookup [coll k not-found]
    (or (aget coll k) not-found))


  ICollection
  (-conj [coll entry]
    (if (vector? entry)
      (-assoc coll (-nth entry 0) (-nth entry 1))
      (loop [ret coll es (seq entry)]
        (if (nil? es)
          ret
          (let [e (first es)]
            (if (vector? e)
              (recur (-assoc ret (-nth e 0) (-nth e 1))
                     (next es))
              (throw (js/Error. "conj on a map takes map entries or seqables of map entries"))))))))

  IAssociative
  (-assoc [coll k v]
    (aset coll k v)
    coll)

  (-contains-key? [coll k]
    (.hasOwnProperty coll k))

  IMap
  (-dissoc [coll k]
    (aset coll k nil)
    coll))

(defn ^mutable game-state [] (GameState.))
