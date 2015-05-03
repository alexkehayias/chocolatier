(ns chocolatier.engine.howler
  "Uses howler.js to handle async loading of sound files and playback")

;; Global controls
;; WTF? Returning the object causes a maximum call stack error!

(defn set-volume! [volume]
  (.volume js/Howler volume)
  nil)

(defn mute! [howler-obj]
  (.mute js/Howler true)
  nil)

(defn unmute! [howler-obj]
  (.mute js/Howler false)
  nil)

;; Samples

(defn play
  "Plays a Howl instance. Optionally pass in a key for which \"audio sprite\" to play

   Example:
   ;; Play the whole sample
   (play (howl [\"/audio/bonfire.mp3\"]))
   ;; Play a section of the sample offset by 1s with duration of 1s
   (play (howl [\"/audio/bonfire.mp3\"] 
               :sprite {\"crackle\" [1000 1000]}) 
        \"crackle\")"
  [howl-obj & [key]]
  (if key
    (.play howl-obj key)
    (.play howl-obj)))

(defn play-multi
  "Play multiple sounds at the same time.

   Example:
   (let [h (howl [\"/audio/bonfire.mp3\"] 
                 :sprite {\"crackle\" [1000 1000]})]
     (play-multi [[howl \"crackle\"]
                  [howl]]))"
  [howl-coll]
  (doseq [[howl & [key]] howl-coll]
    (play howl key)))

(defn howl
  "Returns an instance of Howl"
  [sources & {:keys [autoplay loop volume callback rate pool sprite]
              :or {volume 1.0
                   autoplay false
                   loop false
                   callback nil
                   rate 1.0
                   pool 5
                   sprite {}}}]
  (new js/Howl (clj->js {"src" sources
                         "autoplay" autoplay
                         "loop" loop
                         "volume" volume
                         "onend" callback
                         "rate" rate
                         "pool" 5
                         "sprite" sprite})))
