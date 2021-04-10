;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.medium

  "Either about:
  
   - Targeting a specific platform (Clojure JVM, Clojurescript dev, or Clojurescript
   release)
   - Transcending those platforms
  
   See README for examples."

  {:author "Adam Helinski"}

  #?(:clj (:require [clojure.walk]))
  #?(:cljs (:require-macros [helins.medium :refer [expand*
                                                   target*
                                                   when-compiling*
                                                   when-target*]])))


;;;;;;;;; Deducing the compilation target from a macro's &env


#?(:clj (defonce ^:private -*cljs-mode

  ;; Holds the CLJS target when environment is CLJS.

  (atom nil)))



#?(:clj (defn target

  "Passing a macro's `&env`, determines the target:
  
   - `:clojure`
   - `:cljs/dev`
   - `:cljs/release`"

  [env]

  (if (:ns env)
    (swap! -*cljs-mode
           #(or %
                (case (env :shadow.build/mode)
                  :dev     :cljs/dev
                  :release :cljs/release
                  (if (identical? (get-in @@(requiring-resolve 'cljs.env/*compiler*)
                                          [:options
                                           :optimizations])
                                  :none)
                    :cljs/dev
                    :cljs/release))))
    :clojure)))



#?(:clj (defmacro target*

  "Calls [[target]]. Meant to be used at the REPL, from Clojure/script."

  []

  (target &env)))


;;;;; Testing targets


(defn cljs?

  "Is the [[target]] referring to Clojurescript? (Either `:cljs/dev` or `:cljs/release`)."

  [target]

  (or (= target
         :cljs/dev)
      (= target
         :cljs/release)))


(defn clojure?

  "Is the [[target]] referring to `:clojure`?"

  [target]

  (= target
     :clojure))


;;;;;;;;;; Simplifying outputting code depending on target


#?(:clj (defn ^:no-doc -forbidden-form

  ;; Returns an `ex-info` notifying about access to a feature forbidden in Clojurescript release.
  ;;
  ;; Used by [[not-cljs-release*]].

  [form]

  (ex-info (str "Call forbidden in CLJS advanced build: "
                form)
           {:medium/forbidden (symbol (resolve (first form)))
            :medium/form      form})))



#?(:clj (defmacro not-cljs-release*

  "**Must be used within a macro.**
  
   Given a macro's `&env` and `&form`, throws if the [[target]] is `:cljs/release`.
  
   Useful for forbidding specific features outside of development.
  
   Thrown exception is an `ex-info` with a data map containing:

   | Keyword | Meaning |
   |---|---|
   | :medium/forbidden | The first symbol extracted from the given form |
   | :medium/form | The given form |"

  []

  `(when (identical? (target ~'&env)
                     :cljs/release)
     (throw (-forbidden-form ~'&form)))))



#?(:clj (defmacro when-target*

  "Akin to the standard `when` macro, executes the given forms when the [[target]] matches
   `target-request` (either a [[target]] or a collection of them for matching any)."

  [target-request & form+]

  (let [target' (target &env)]
    (when (if (coll? target-request)
            (some #(identical? %
                               target')
                  target-request)
            (identical? target'
                        target-request))
      `(do ~@form+)))))


;;;;;;;;;; Anonymous macros


#?(:clj (defn- -resolve-target

  ;; In `form+`, resolves the special symbol '&target to the actual target.

  [target form+]

  (clojure.walk/postwalk #(if (= %
                                 '&target)
                            target
                            %)
                         form+)))



#?(:clj (defmacro expand*

  "Also known as an \"anonymous macro\".
  
   Whether in Clojure JVM or during Clojurescript compilation, the given forms
   are executed as Clojure JVM and the returned value is expanded just like in
   a macro.

   See README for a full example."

  [& clojure-form+]

  (let [target-now (target &env)
        form       `(do
                      ~@(-resolve-target target-now
                                         clojure-form+))]
    (if (identical? target-now
                    :clojure)
      `(eval ~form)
      (do
        (require (ns-name *ns*))
        (eval form))))))



#?(:clj (defmacro when-compiling*

  "Similar to [[expand*]] but any returned value is discarded.
  
   Solely useful for any kind of side effect on the JVM regardless of the target
   platform."

  [& clojure-form+]

  (let [target-now (target &env)
        form       `(do
                      ~@(-resolve-target target-now
                                         clojure-form+)
                      nil)]
    (if (identical? target-now
                    :clojure)
      form
      (do
        (require (ns-name *ns*))
        (eval form))))))
