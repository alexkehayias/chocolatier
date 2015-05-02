(ns chocolatier.dev
    (:require [chocolatier.core :refer [restart-game!]]
              [figwheel.client :as fw]))

(fw/start {
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :on-jsload restart-game!})
