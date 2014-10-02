(ns chocolatier.dev
  (:require [cemerick.austin :as brepl-env]
            [cemerick.austin.repls :as brepl]
            [cemerick.austin :as aus]
            [cemerick.austin.repls :refer (browser-connected-repl-js)]))


(defn reset-brepl-env! []
  (reset! brepl/browser-repl-env (brepl-env/repl-env)))

(defn connect-to-brepl [repl-env]
  (brepl/cljs-repl repl-env))


;; HACK to get access to repl client js
(defn repl-client-js [session-id]
  (if-let [session (get (deref @#'aus/sessions) session-id)]
    (slurp @(:client-js session))
    (format ";console.error('Austin ClojureScript REPL session %s does not exist. Maybe you have a stale ClojureScript REPL environment in `cemerick.austin.repls/browser-repl-env`?');"
            session-id)))

(defn get-session-id [brepl-js]
  (get (clojure.string/split brepl-js #"/") 3))

(defn get-repl-client-js []
  (repl-client-js (get-session-id (browser-connected-repl-js))))

