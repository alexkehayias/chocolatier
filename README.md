# chocolatier

A work-in-progress web game engine for repl driven game development written in ClojureScript using Pixi.js as a rendering engine.

## Demo

Here's the working example game that includes tilesets, collision detection, animation, sprites, and user input.

![Alt text](/../screenshots/gameplay_1.gif?raw=true "Chocolatier engine gameplay")

## Usage

### With `figwheel`
The following instructions will start a browser connected repl and launch the demo game:

0. Clone the project and all submodules `git clone --recursive https://github.com/alexkehayias/chocolatier`
1. Start the browser REPL server `lein figwheel`
2. Navigate your browser to `http://127.0.0.1:1223/dev` to connect to the REPL and view devcards for the project
3. Play the example game at `http://127.0.0.1:1223/dev#!/chocolatier.examples.action_rpg.core` changes to files will automatically restart the game

### With `figwheel` and emacs using `cider`

The recommended setup is to connect to the `figwheel` REPL server from emacs `cider` so that you can integrate the text editor into the development workflow. This allows you to send code to the running repl for evaluation using emacs `cider-mode`.

After completing step 2 from the `figwheel` instructions above, in emacs:

1. Connect to the `figwheel` REPL `M-x cider-connect RET localhost RET 8999`
2. Start ClojureScript REPL `(do (use 'figwheel-sidecar.repl-api) (cljs-repl))`

### Compiling with advanced optimizations

1. Run `lein cljsbuild once min`
2. Navigate your browser to `http://127.0.0.1:1223/min` and the game will start immediately

## Entity Component System

The game engine implemented using a modified entity component system which organizes aspects of a game modularly. Think about it less as a bunch of objects with their own state and methods and more like a database where you query for functionality, state, based on a certain aspect or entity.

Organization:

1. Scene - collection of system labels to be looked up and called by the game loop (main menu, random encounter, world map, etc)
2. System - functions that operates on a component or not and returns updated game state. Examples: input, rendering, collision detection
3. Components - functions that return updated component state per entity and events to emit
4. Entities - unique IDs that have a list of components to they participate in. Examples: `{:player1 [:controllable :moveable :destructable]}`

### Example

The following example implements a simple game loop, middleware, system, component, and entities to show you how it all fits together. See the rest of the game for a more in-depth example (and graphics).

```clojure
(ns user.test
  (:require [chocolatier.engine.ecs :as ecs]
            [chocolatier.engine.core :refer [game-loop mk-game-state]]))

(defn test-component-fn
  "Increment the :x value by 1"
  [entity-id component-state inbox]
  (println entity-id component-state)
  (update component-state :x inc))

(defn init-state
  "Initial game state with our example system, component, and a few entities"
  []
  (mk-game-state {} [:scene :test-scene [:test-system]]
                    [:current-scene :test-scene]
                    [:system :test-system :testable test-component-fn]
                    [:entity :player1 [[:testable {:x 0 :y 0}]]]
                    [:entity :player2 [[:testable {:x 10 :y 10}]]]))

(defn run-n-frames
  "Middleware to count the number of frames and return nil to indicate
  the game loop should exit after n frames"
  [f n]
  (fn [state]
    (when (<= (:frame-count state 0) 10)
      (update (f state) :frame-count inc))))

(defn run
  "Defines the function that is called each time the game loop runs.
  You can add additional middleware here similar to ring handlers."
  [handler]
  (-> handler
      (run-n-frames 10)))

;; Run the game loop 10 times and print the component-state each frame
(game-loop (init-state) run)

```

## State

The game is represented as a hashmap and a collection of functions that transform the state. This makes it easy to test game logic by calling functions with mocked data (since it's just a hashmap). You should be able to test any system, component, etc with data only.

## Browser Connected Repl (Brepl)

A browser repl is automatically available when the server is started when using `lein figwheel`. This allows you to dynamically re-evaluate the code running in the browser without a page refresh. Static files can also watched and reload the game when changed. See the `figwheel` documentation for more.

## Cross-component communication

A global pub-sub event queue is available for any component enabling cross component communication without coupling the state of any of the components. For example, suppose the render component needs to update the screen position of the player sprite. The render component needs information from the input component, but we don't want to couple the state of either components together. Instead of directly accessing the input component's state from the render component we subscribe to messages about player movement and update based on that. We can broadcast that information without any knowledge of who is listening to it and no one can change the component state from another component.

### Events

By default, component functions created with `ecs/mk-component` can output a single value, representing component state, or two values, component state and a collection of events to emit.

For example, the following component will emit a single event called `:my-event` with the message `{:foo :bar}`:

```clojure
(defn component-a [entity-id component-state inbox]
  [component-state [(ev/mk-event {:foo :bar} [:my-event entity-id])]]])
```

### Subscriptions

Any component can subscribe to events by creating a component with a `:subscriptions` key in the options hashmap where each subscription is a vector of selectors:

```clojure
(mk-component state [component-f {:subscriptions [:action :movement]}])
```

The subscribed component will receive the event in a hashmap in the `:inbox` key in the context argument (third argument) to the component function. Messages that are sent are available immediately to the subscriber which allows for events to be sent and received within the same frame and are therefore order dependent.

## Tilemaps

The game engine supports tilemaps generated from the Tiled map editor http://www.mapeditor.org. Export the map as json and include the tileset image in the `resources/public/static/images` directory.

Tilemaps require all assets to be loaded (tileset images) to prevent any race conditions with loading a tilemap see `chocolatier.engine.systems.tiles/load-assets`. Tilemaps are loaded asynchronously from the server via `chocolatier.engine.systems.tiles/load-tilemap` which takes a callback.

## Middleware

The game loop can be wrapped in middleware similar to `ring` middleware. This provides a way of accessing the running game state, ending the game loop, introspection, and other side effects.

Here's an example of a middleware that makes a running game's state queryable in the repl:

```clojure
(defn wrap-copy-state-to-atom
  "Copy the latest game state to the copy-atom so it can be inspected outside
   the game loop."
  [f copy-atom]
  (fn [state]
    (let [next-state (f state)]
      (reset! copy-atom next-state)
      next-state)))
```

Usage:

```
(def *state* (atom nil))

(game-loop state (fn [handler]
                   (-> handler
                       (wrap-copy-state-to-atom *state*))))

(println (-> *state* deref keys))
```


## Running Tests

View the tests using the devcards at `http://127.0.0.1:1223/dev`

## Performance

The game engine is being tested to get to 100 "game objects" with meaningful functionality, tilemaps, sound, etc at 60 FPS. Performance tuning is an ongoing process and the project is still being thoroughly optimized. ClojureScript presents challenges for optimization including garbage collection, persistent data structures, and functional paradigms that js engines may have difficulty optimizing.

### Tips

Here are some tips for optimizing performance of game loops:

- Use the Chrome dev tools to do a CPU profile and trace where time is being spent
- Don't use `partial` or `apply` as they are slow
- Always specify each arrity of a function instead of using `(fn [& args] ...)`
- Don't use `multimethod`, use `cond` or `condp` and manually dispatch
- Use `array` when you need to quickly append items to a collection (mutable)
- Use `loop` instead of `for` or `into` with transients or arrays as the accumulator
- Avoid boxing and unboxing i.e multiple maps/fors over a collection, use transducers, reducers or loops
- Don't make multiple calls to get the same data, put it in a `let`
- Avoid heavily nested closures as the lookup tree becomes very long and slow
- Favor eager operations over lazy operations i.e `reduce` instead of `for`
- Don't use `concat` or `mapcat` as they can be slow and generate lots of garbage
- Don't use `last` as it will need to traverse the whole sequence, use `nth` instead if you know how many elements are in the collection
- Don't use hashmaps as functions `({:a 1} :a)`, instead use `get` or keywords as functions
- Always return the same type from a function (V8 can then optimize it)

### Advanced Compilation

The `min` build uses advanced compilation with static function inlining which can nearly substantially increase the framerate in most instances.

### Benchmarks

Naive frames per second benchmarks are available at `chocolatier.engine.benchmarks` for measuring the performance of the framework.

## License

Copyright © 2016 Alex Kehayias

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
