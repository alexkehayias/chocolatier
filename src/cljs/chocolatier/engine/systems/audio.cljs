(ns chocolatier.engine.systems.audio
  "Provides a global audio system that takes events and plays the sample."
  (:require [chocolatier.engine.howler :as sound]
            [chocolatier.engine.systems.events :as ev]))


(defn get-or-create-sample
  "Check if a sample exists in the library hashmap, if not create a howl instance"
  [audio-directory library sample-id]
  (or (:sample-id library)
      (sound/howl [(str audio-directory "/" (name sample-id) ".mp3")])))

;; TODO use an audio sprite where the caller declares the name of the
;; source file and the start, end playheads to return minimum number
;; of Howl instances
(defn load-samples
  "Returns a hashmap of :sample-id howl instances and calls function callback
   with the results. This will create n number of howl instances where 
   n is the number of sample-ids.

   Example:
   (load-samples \"/audio\" [:drip] #(sound/play (:drip %))"
  [audio-directory sample-ids callback]
  (callback
   (reduce #(assoc %1 %2 (get-or-create-sample audio-directory
                                               %1 %2))
           {}
           sample-ids)))

(defn audio-system
  "Initialized with a samples library created with load-samples. 

   To use the audio system, emit an event with the :audio selector
   and a :msg with :sample-id which will be looked up in the samples library.
   
   Throws an exception if the sample is not found in the library"
  [samples-library]
  (fn [state]
    (let [;; Deduplicate any events for the exact same sounds
          events (set (ev/get-events state [:audio]))
          sample-ids (map #(get-in %1 [:msg :sample-id]) events)]
      (doseq [id sample-ids]
        (if-let [sample (get samples-library id)] 
          (sound/play sample)
          (throw (js/Error. (str "Could not find sample " id " in library")))))
      (assoc-in state [:game :audio :library] samples-library))))

