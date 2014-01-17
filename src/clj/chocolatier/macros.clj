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
