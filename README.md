# chocolatier

A work-in-progress web game engine for repl driven game development written in ClojureScript using Pixi.js as a rendering engine.

## Usage

1. Load `chocolatier.server` to start the test server and a browser repl
2. Go to http://127.0.0.1:9000/app
3. Load the namespace `chocolatier.engine.core` and call `(start-game!)`

## Entity Component System

The game engine implemented using a modified entity component system which organizes aspects of a game modularly. Think about it less as a bunch of objects with their own state and methods and more like a database where you query for functionality, state, based on a certain aspect or entity.

Organization:

1. Scene - collection of system functions called by the game loop (main menu, random encounter, world map, etc)
2. System - functions that operates on a component and returns updated game state. Examples: input, rendering, collision detection
3. Components - hold state and component functions relating to a certain aspect. Polymorphism can be used to dispatch on entity IDs (or however else you want) for finer control. Examples: moveable, user controllable, collidable, destructable
4. Entities - unique IDs that have a list of components to they participate in. Examples: `{:player1 [:controllable :moveable :destructable]}`

### Example

The following example implements a simple game loop, system, component, and entities to show you how it all fits together. See the rest of the game for a more in-depth example (and graphics).

```clojure
(defn game-loop
  "Simple game loop that runs 10 times and returns the state on the last frame."
  [state scene-id frame-count]
  (if (< frame-count 10)
    (recur (ces/iter-fns state (ces/get-system-fns state (-> state :scenes scene-id)))
           scene-id
           (inc frame-count))
    state))

(defn test-system
  "Call all the system functions and merge their changes together"
  [state component-fns entity-ids]
  (apply ces/deep-merge (for [f fns, e entity-ids]
                          (f state e))))
                            
(defn test-fn
  "Increment the :x value by 1"
  [component-state entity-id]
  (assoc component-state :x (inc (:x component-state))))

(defn my-game
  "Test the entire CES implementation with a system that changes component state"
  []
  (let [init-state (-> {}
                       (ces/mk-scene :test-scene [:test-system])
                       (ces/mk-system :test-system test-system :testable)
                       (ces/mk-entity :player1 [:testable])
                       (ces/mk-entity :player2 [:testable])
                       (ces/mk-component :testable [test-fn]))]
        (game-loop init-state :test-scene 0)))
```

## State

The game is represented as a hashmap and a collection of functions that transform the state. This makes it easy to test game logic by calling functions with mocked data (since it's just a hasmap). You should be able to test any system, component, etc with data only.

## Browser Connected Repl (Brepl)

A browser repl is automatically available when the server is started. This allows you to dynamically re-evaluate the code running in the browser without a page refresh. Keep in mind that by refreshing the page, if you are using a compiled js file, you will need to re-evaluate any code that you have changed or recompile the project `lein cljsbuild once`.

Any changes to a running browser game take effect immediately on the next frame. Just about everything can be re-evaluated on the fly. For example, changing the moving rate of the player and evaluating the code in the browser repl will show the change in movement rate right away!

## Running Tests

Currently does not support `lein-cljsbuild` tests. Instead, load a namespace in a brepl and use the `test-ns` macro to run the tests.

## License

Copyright © 2014 Alex Kehayias

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
