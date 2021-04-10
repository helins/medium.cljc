;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.medium

  ""

  {:author "Adam Helinski"}

  #?(:clj (:require [clojure.walk]))
  #?(:cljs (:require-macros [helins.medium :refer [expand*
                                                   target*
                                                   when-compiling*
                                                   when-target*]])))


;;;;;;;;; Deducing the compilation target from a macro's &env


#?(:clj (defonce ^:private -*cljs-mode

  ;;

  (atom nil)))



#?(:clj (defn target

  ""

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

  ""

  []

  (target &env)))


;;;;;


(defn cljs?

  ""

  [target]

  (or (= target
         :cljs/dev)
      (= target
         :cljs/release)))


(defn clojure?

  ""

  [target]

  (= target
     :clojure))


;;;;;;;;;; Macros simplifying outputting code depending on target


#?(:clj (defn not-cljs-release

  ""

  [env form]

  (when (identical? (target env)
                    :cljs/release)
    (throw (ex-info (str "Call forbidden in CLJS advanced build: "
                         form)
                    {:medium/forbidden (symbol (resolve (first form)))
                     :medium/form      form})))))



#?(:clj (defn- -when-requested-target

  ""

  [target-request target form+]

  (when (if (coll? target-request)
          (some #(identical? %
                             target)
                target-request)
          (identical? target
                      target-request))
    `(do ~@form+))))



#?(:clj (defmacro when-target*

  ""

  [target-request & form+]

  (-when-requested-target target-request
                          (target &env)
                          form+)))


;;;;;;;;;; Anonymous macros


#?(:clj (defn- -resolve-target

  ;;

  [target form+]

  (clojure.walk/postwalk #(if (= %
                                 '&target)
                            target
                            %)
                         form+)))



#?(:clj (defmacro expand*

  ""

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

  ""

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
