(ns chocolatier.macros
  (:require [cljs.compiler :refer (munge)])
  (:refer-clojure :exclude (munge defonce)))


;; Via cemerick https://gist.github.com/cemerick/6331727
(defmacro defonce
  [vname expr]
  (let [ns (-> &env :ns :name name munge)
        mname (munge (str vname))]
    `(when-not (.hasOwnProperty ~(symbol "js" ns) ~mname)
       (def ~vname ~expr))))

(defmacro defcomponent
  "Creates a component with name state and any number of methods

   Example:
   (defcomponent Moveabe [:x :y]
     (move [this] nil))"
  [vname state & methods]
  `(do
     ;; Create the protocol with methods
     (defprotocol ~vname ~@methods)
     ;; Metadata can't be attached to a protocol so rebind the newly
     ;; created protocol to a var with metadata describing it's state
     (def ~vname
       (with-meta ~vname {:fields ~state}))))

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
  (let [;; Protocols are not seqs so we can filter by that
        components (filter #(not (seq? %)) body)
        symbolize #(map symbol %)
        namify #(map name %)
        extract #(-> % meta :fields)
        parse-fn #(-> % eval extract namify symbolize)
        fields (vec (reduce #(concat %1 (parse-fn %2)) [] components))]
    ;; Validate that all fields are unique or throw an error
    (if (= (count fields)  (count (set fields)))
      ;; Create the defrecord with component protocols
      `(defrecord ~vname [~@fields]
         ~@body)
      (throw (Exception. (str "Duplicate fields found across components. "
                              "Fields must all be unique."))))))
