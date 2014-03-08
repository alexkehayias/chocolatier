# chocolatier

A work-in-progress web game engine for repl driven game development written in ClojureScript using Pixi.js as a rendering engine.

## Usage

1. Compile the javascript using `lein cljsbuild once`
2. Load `chocolatier.server` to start the test server and a browser repl
3. Go to http://127.0.0.1:9000/app to load the game

## Entity Component System

An entity component system is implemented using `defrecord` and `defprotocol`. Changes to defprotocols or defrecords take effect immediately since all system transactions produce new records.

When including protocols from a separate namespace, refer them explicitely by name. You do not need to refer their method names. When calling method names, refer the namespace containing the method you want to use using `refer :as`.

## Brepl

A browser repl is aautomatically available when the server is started. This allows you to dynamically re-evaluate the code running in the browser without a page refresh. Keep in mind that by refreshing the page, if you are using a compiled js file, you will need to re-evaluate any code that you have changed or recompile the project `lein cljsbuild once`. 

Any changes to a running browser game take effect immediately on the next frame. All entities and components can be re-evaluated on the fly. For example, changing the moving rate of the player and evaluating the code in the browser repl will show the change in movement rate right away. This can be extremely useful when building the game.

## Notes

- The game seems to render choppy when the js console is open (Chrome, MacbookAir mid 2011)

## License

Copyright © 2014 Alex Kehayias

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.