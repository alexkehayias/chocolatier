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


;; protocols can have more than one method

;; state is a list of keywords
(defmacro defcomponent
  "Creates a component with name state and methods"
  [vname state methods]
  `(do
     ;; Create the protocol with methods
     (defprotocol ~vname ~methods)
     ;; Metadata can't be attached to a protocol so rebind the newly
     ;; created protocol to a var with metadata describing it's state
     (def ~vname
       (with-meta ~vname {:fields ~state}))))

;; (defmacro defentity
;;   "A collection of components that represent all aspects of what 
;;    the entity can do"
;;   [vname & body]
;;   (let [;; Every odd form in body is a Protocol
;;         components (map first (partition-all 2 body))
;;         _ (println "Components" components)
;;         ;; This gives us a list of symbols, not protocols!
;;         ;; We need to evaluate each protocol
;;         fields (map #(symbol (-> % meta :fields)) components)]
;;     ;; Create the defrecord with component protocols
;;     `(defrecord ~vname ~fields)))


;; Creates a defrecord with a list of methods and adds all the
;; required state as namespaced keywords on the record
;; :comp-id/:var
;; :moveable/:x 1 :moveable/:y 1
;; That way all component state for an entity can be accessed in
;; methods via `this`
;; Global state can be just a list of entities and systems

;; (defentity Player
;;   Movable
;;   (move [this state component]))

;; Maybe need to add meta data to the component so that the defentity
;; knows what fields it needs to add
;; (defcomponent Moveable
;;   (move [this state component]))

;; (defmacro defentity
;;   [vname methods]
;;   `(defrecord ~vname
;;        `(for [m methods]
;;           m)))
