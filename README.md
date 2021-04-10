# Medium, targeting or transcending CLJC targets

[![Clojars](https://img.shields.io/clojars/v/io.helins/medium.svg)](https://clojars.org/io.helins/medium)

[![Cljdoc](https://cljdoc.org/badge/io.helins/medium)](https://cljdoc.org/d/io.helins/medium)

![CircleCI](https://circleci.com/gh/helins/medium.cljc.svg?style=shield)

[Reader conditionals](https://clojure.org/guides/reader_conditionals) allow
for selecting code depending on whether the compilation target is Clojure JVM or
Clojurescript.

However, for some use cases, they fall short. Especially when a macro must
decide what to return depending on the compilation target ; reader conditionals
cannot be used in that way.

Even further, if the target is Clojurescript, a macro might even need to
distinguish between development and release.

This small library offers straightforward tooling for such endaveaours as well
as a few extra CLJC utilities.

## Usage

The [full API is available on Cljdoc](https://cljdoc.org/d/io.helins/medium).

First, requiring the core namespace:

```clojure
(require '[helins.medium :as medium])
```

### Identifying the compilation target

A compilation target is a keyword such as:

| Target | Description |
|---|---|
| `:clojure` | Clojure JVM |
| `:cljs/dev` | Clojurescript development |
| `:cljs/release` | Clojurescript release |

It is deduced by probing for Shadow-CLJS or the regular Clojurescript compiler.

At the REPL, the target can be queried as such:

```clojure
(def target
     (medium/target*))
```

When writing macros, the related function should be used with the macro
environment (special symbol `&env`):

```clojure
(defmacro my-macro
  []
  (case (medium/target &env)
    :clojure      ...
    :cljs/dev     ...
    :cljs/release ...))
```

Outside of macros, `when-target*` returns the given forms for expansion only if
the target is matched:

```clojure
;; When :cljs/release, do ...
;;
(medium/when-target* :cljs/release
  (println "Only for release!"))


;; When not :cljs/release, do ...
(medium/when-target* [:clojure
                      :cljs/dev]
  ...)
```

### Forbidding features in `:cljs/release`

Authors, especially library authors, should actively strive for ensuring good dead
code removal during advanced Clojurescript compilation. Or, simply, some features are
meant only for development, not for release.

The following ensures a forbidden macro call does not happen.

If this macro detects the `:cljs/release` target, it will throw during
Clojurescript compilation:

```clojure
(defmacro my-macro-2
  []
  (medium/not-cljs-release*)
  ...)
```

### Anonymous macros

By using `expand*`, one can execute code as Clojure JVM, regardless of the
compilation target, and the result is expanded in the source.

Here is an example from a real-world project which is used for creating many
`(def)` forms meant to represent a gradient of CSS color using the [Garden
library](https://github.com/noprompt/garden):


```clojure
(medium/expand*
  `(do
     ~@(map (fn [lightness]
              `(def ~(symbol (str "bw-"
                                  lightness))
                    (garden.color/hsl 0 0 ~lightness)))
            (range 0
                   100
                   5))))
```

When all that matters is applying side effects from the JVM, `when-compiling*`
can be used and will discard the returned result, nothing ends up in the source:

```clojure
(medium/when-compiling*
  (println "Always called from the JVM! Even when compiling CLJS!")
  (println "Proof: " *clojure-version*))
```


## Running tests

Clojure, on the JVM, using [Kaocha](https://github.com/lambdaisland/kaocha):

```bash
$ ./bin/test/jvm/run
```
Clojurescript, on NodeJS, using [Shadow-CLJS](https://github.com/thheller/shadow-cljs):

```bash
$ ./bin/test/node/run

# Or testing an advanced build:
$ ./bin/test/node/advanced
```


## Development

Starting in Clojure JVM mode, mentioning an additional Deps alias (here, a local
setup of NREPL):
```bash
$ ./bin/dev/clojure :nrepl
```

Starting in CLJS mode using [Shadow-CLJS](https://github.com/thheller/shadow-cljs):
```bash
$ ./bin/dev/cljs
# Then open ./cljs/index.html
```


## License

Copyright © 2021 Adam Helinski

Licensed under the term of the Mozilla Public License 2.0, see LICENSE.
