(ns chocolatier.engine.rtree)


(defn rtree
  ([]
   (js/rbush))
  ([max-entries]
   (js/rbush max-entries)))

(defn rtree-item
  ([x1 y1 x2 y2]
   (rtree-item x1 y1 x2 y2 nil))
  ([x1 y1 x2 y2 data]
   (array x1 y1 x2 y2 data)))

(defn rtree-insert!
  [rtree item]
  (.insert rtree item))

(defn rtree-bulk-insert!
  [rtree items]
  (.load rtree items))

(defn rtree-remove!
  [rtree item]
  (.remove rtree item))

(defn rtree-clear!
  [rtree]
  (.clear rtree))

(defn rtree-collides?
  [rtree item]
  (.collides rtree item))

(defn rtree-search
  [rtree item]
  (.search rtree item))

(defn rtree-all
  [rtree]
  (.all rtree))
