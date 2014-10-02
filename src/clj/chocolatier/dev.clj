(ns chocolatier.dev
  (:require [cemerick.austin :as brepl-env]
            [cemerick.austin.repls :as brepl]
            [cemerick.austin :as aus]
            [cemerick.austin.repls :refer (browser-connected-repl-js)]))


(defn reset-brepl-env! []
  (reset! brepl/browser-repl-env (brepl-env/repl-env)))

(defn connect-to-brepl [repl-env]
  (brepl/cljs-repl repl-env))

;; HACK This circumvents the need to have clojure.browser.repl
;; required by a compiled js file. Otherwise it causes race conditions
;; and may not load the brepl at all after the page is loaded.

;; After reading through the austin source code we see that the server
;; <session-id>repl/start url always works and it always has a script
;; tag before the browser repl js that looks like all the goog code
;; for dependencies. Having goog present fixes it all up.
(defn repl-client-js [session-id]
  (if-let [session (get (deref @#'aus/sessions) session-id)]
    (slurp @(:client-js session))
    (format ";console.error('Austin ClojureScript REPL session %s does not exist. Maybe you have a stale ClojureScript REPL environment in `cemerick.austin.repls/browser-repl-env`?');"
            session-id)))

(defn get-session-id [brepl-js]
  (get (clojure.string/split brepl-js #"/") 3))

(defn get-repl-client-js []
  (repl-client-js (get-session-id (browser-connected-repl-js))))

