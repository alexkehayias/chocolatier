(ns chocolatier.macros)


(defmacro defcomponent
  "Creates a component with name state and any number of methods

   Example:
   (defcomponent Moveable [:x :y]
     (move [this] nil))"
  [vname state & methods]
  `(do
     ;; Create the protocol with methods in a hashmap along with
     ;; fields since clojurescript does not do metadata on defs
     (defprotocol ~vname ~@methods)
     (def ~vname
       {:fields ~state
        :component ~vname})))

(defmacro defentity
  "A collection of components that represent all aspects of what 
   the entity can do. Component fields must be unique across all
   components included in the entity.

   Example:
   (defentitiy Player
     Moveable
     (move [this] nil)
     (stop [this] nil)

     Renderable
     (render [this] nil))
  "
  [vname & body]
  (let [;; Protocols are not seqs so we can filter out lists
        components (filter #(not (seq? %)) body)
        symbolize #(map symbol %)
        namify #(map name %)
        parse-fn #(-> % eval :fields namify symbolize)
        fields (vec (reduce #(concat %1 (parse-fn %2)) [] components))
        ;; Protocols are in a hashmap key :components that we
        ;; must coerce out. The :on key can be symbolized
        parsed-body (for [f body]
                      (if-not (seq? f)
                        (-> f eval :component :on symbol)
                        f))]
    ;; Validate that all fields are unique or throw an error
    (if (= (count fields) (count (set fields)))
      ;; Create the defrecord with component protocols
      `(defrecord ~vname [~@fields] ~@parsed-body)
      (throw (Exception. (str "Duplicate fields found across components. "
                              "Fields must all be unique."))))))

(defmacro forloop [[init test step] & body]
  "For loop implementation. Example:
   (forloop [[i 0] (< i 16) (inc i)] (println i))"
  `(loop [~@init]
     (when ~test
       ~@body
       (recur ~step))))

;; LOCALS

;; Optimized locals that can be used as a mutable piece of state
;; inside local scope
(defmacro local
  ([]
   `(make-array 1))
  ([x]
   `(cljs.core/array ~x)))

(defmacro >> [x v]
  `(aset ~x 0 ~v))

(defmacro << [x]
  `(aget ~x 0))

(defmacro forloop-accum
  "For loop with accumulation into a transient vector"
  [[init test step] & body]
  `(loop [~@init
          res# (transient [])]
     (if ~test
       (do (conj! res# ~@body)
           (recur ~step res#))
       (persistent! res#))))
