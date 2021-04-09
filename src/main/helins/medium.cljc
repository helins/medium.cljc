;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.medium

  ""

  {:author "Adam Helinski"}

  #?(:clj (:require [clojure.edn]
                    [clojure.string]
                    [clojure.walk]))
  #?(:cljs (:require [clojure.string]))
  #?(:cljs (:require-macros [helins.medium :refer [expand*
                                                   load-edn*
                                                   load-string*
                                                   target*
                                                   touch-recur*
                                                   when-compiling*
                                                   when-target*]]))
  #?(:clj (:import java.io.File)))


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


;;;;;;;;;; Macros simplify outputting code depending on target


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


;;;;;;;;;; Testing file extensions


(defn file-cljc?

  ""

  [filename]

  (clojure.string/ends-with? filename
                             ".cljc"))



(defn file-cljs?

  ""

  [filename]

  (or (file-cljc? filename)
      (clojure.string/ends-with? filename
                                 ".cljs")))



(defn file-clojure?

  ""

  [filename]

  (or (file-cljc? filename)
      (clojure.string/ends-with? filename
                                 ".clj")))


;;;;;;;;;; Touching files (mainly for forcing recompilation in watching compilers)


#?(:clj (defn touch-recur

  ""

  ([path]

   (touch-recur path
                nil))


  ([path pred]

   (let [now    (System/currentTimeMillis)
         pred-2 (or pred
                    (fn [_]
                      true))]
     (into []
           (comp (map (fn [^File file]
                        (let [path (.getCanonicalPath file)]
                          (when (pred-2 path)
                            (.setLastModified file
                                              now)
                            path))))
                 (filter some?))
           (file-seq (File. path)))))))



#?(:clj (defmacro touch-recur*

  ""

  ([path]

   `(touch-recur* ~path
                  nil))


  ([path pred]

   (touch-recur path
                (eval pred)))))


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


;;;;;;;;;; Loading and expanding content from files


#?(:clj (defn load-edn

  ""

  [path]

  (clojure.edn/read-string (slurp path))))



#?(:clj (defmacro load-edn*

  ""

  [path]

  `(quote ~(load-edn path))))



#?(:clj (defmacro load-string*

  ""

  [path]

  (slurp path)))
