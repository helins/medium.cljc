(ns helins.medium

  ""

  {:author "Adam Helinski"}

  #?(:clj (:require [cljs.env]
                    [clojure.tools.namespace.repl]))
  #?(:cljs (:require-macros [helins.medium :refer [cljs-compiler-optimization*
                                                   ;expand*
                                                   refresh-clojure*
                                                   target*
                                                   target-init*]])))


#?(:clj (clojure.tools.namespace.repl/disable-reload!))


;;;;;;;;;; Flags


#?(:clj

(def ^:dynamic *force-clojure?*

  ""

  false))



#?(:clj

(def ^:dynamic *refresh-clojure?*

  ""

  true))


;;;;;;;;;; Extracting information from the Clojurescript compiler

   
#?(:clj

(defn cljs-compiler

  ""

  []

  (some-> cljs.env/*compiler*
          deref)))



#?(:clj

(defn cljs-compiler-optimization

  ""

  []

  (get-in (cljs-compiler)
          [:options
           :optimizations])))



(defmacro cljs-compiler-optimization*

  ""

  []

  (cljs-compiler-optimization))


;;;;;;;;; Detecting the target (at initialization and currently)

#?(:clj

(defn target-init

  ""

  []

  (if-some [level-raw (cljs-compiler-optimization)]
    (if (identical? level-raw
                    :advanced)
      :cljs/release
      :cljs/dev)
    :clojure)))


(defmacro target-init*

  ""

  []

  (target-init))



#?(:clj

(defn target

  ""

  []

  (if *force-clojure?*
    :clojure
    (target-init))))



(defmacro target*

  ""

  []

  (target))


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


;;;;;;;;;; Refreshing Clojure files from CLJS


#?(:clj

(defn refresh-clojure

  ""

  ;; TODO. Disable when already in Clojure?

  []

  (when *refresh-clojure?*
    (binding [*force-clojure?*   true
              *refresh-clojure?* false
              clojure.core/*e    nil]
      (clojure.tools.namespace.repl/refresh)
      (some-> clojure.core/*e
              throw)))
  nil))



(defmacro refresh-clojure*

  ""

  []

  (refresh-clojure))


;;;;;;;;;; Anonymous macros


;(defmacro expand*
;
;  ""


