(ns chocolatier.engine.components.text)


(defn mk-text-state
  [text radians]
  {:text text :rotation radians})

(defn get-text-change-event [inbox]
  (some #(when (keyword-identical? (:event-id %) :text-change) %) inbox))

(defn text
  "Check if there are any text events, sets the text that will be shown"
  [entity-id component-state {:keys [inbox]}]
  (if-let [text-event (:msg (get-text-change-event inbox))]
    (assoc component-state :text (:text text-event))
    component-state))
