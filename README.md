# chocolatier

A work-in-progress web game engine for repl driven game development written in ClojureScript using Pixi.js as a rendering engine.

## Usage

1. Compile the javascript using `lein cljsbuild once`
2. Load `chocolatier.server` to start the test server and a browser repl
3. Load `chocoloatier.engine.core` and call `(reset-game!)` to start the game

## Entity Component System

An entity component system is implemented using `defrecord` and `defprotocol`. Changes to defprotocols or defrecords take effect immediately since all system transactions produce new records.

When including protocols from a separate namespace, refer them explicitely by name. You do not need to refer their method names. When calling method names, refer the namespace containing the method you want to use using `refer :as`.

## License

Copyright © 2013 Alex Kehayias

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.