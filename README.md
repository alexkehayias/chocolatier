# chocolatier

A work-in-progress web game engine for repl driven game development written in ClojureScript using Pixi.js as a rendering engine.

## Usage

Load `chocolatier.server` to start the test server and a browser repl. Load `chocoloatier.engine.core` and call `(reset-game!)` to start the game.

## Entity Component System

An entity component system is implemented using `defrecord` and `defprotocol`. When loading changes to protocols or records in the repl make sure to delete any old instances and replace with new ones for the changes to take effect. 

When including protocols from a separate namespace, refer them explicitely by name. You do not need to refer their method names. When calling method names, refer the namespace containing the method you want to use using `refer :as`.

## License

Copyright © 2013 Alex Kehayias

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.