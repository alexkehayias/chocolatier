(ns chocolatier.dev
  (:require [cemerick.austin :as brepl-env]
            [cemerick.austin.repls :as brepl]))


(defn reset-brepl-env! []
  (reset! brepl/browser-repl-env (brepl-env/repl-env)))

(defn connect-to-brepl [repl-env]
  (brepl/cljs-repl repl-env))

