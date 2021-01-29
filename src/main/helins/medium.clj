(ns helins.medium

  ""

  {:author "Adam Helinski"}

  (:require [cljs.env]
            [clojure.tools.namespace.repl]))
  ;#?(:cljs (:require-macros [helins.medium :refer [base-cljs?
  ;                                                 base-clojure?
  ;                                                 cljs-level
  ;                                                 cljs-level-raw
  ;                                                 compile-cljs?
  ;                                                 compile-clojure?
  ;                                                 force-clojure
  ;                                                 force-clojure?
  ;                                                 refresh-clojure
  ;                                                 ]])))


(clojure.tools.namespace.repl/disable-reload!)


;;;;;;;;;; Miscellaneous

   
(defn cljs-compiler

  ""

  []

  (some-> cljs.env/*compiler*
          deref))


;;;;;;;;;; CLJS compilation levels


(defn cljs-level-raw

  ""

  []

  (get-in (cljs-compiler)
          [:options
           :optimizations]))



(defmacro cljs-level-raw*

  ""

  []

  (cljs-level-raw))



(defn cljs-level

  ""

  []

  (when-some [level-raw (cljs-level-raw)]
    (if (identical? level-raw
                    :advanced)
      :release
      :dev)))



(defmacro cljs-level*

  ""

  []

  (cljs-level))


;;;;;;;;;; Detecting the base platform


(defn base-cljs?

  ""

  []

  (some? (cljs-compiler)))



(defmacro base-cljs?*

  ""

  []

  (base-cljs?))



(defn base-clojure?

  ""

  []

  (nil? (cljs-compiler)))



(defmacro base-clojure?*

  ""

  []

  (base-clojure?))


;;;;;;;;;; Refreshing Clojure files from CLJS


(def ^:dynamic *force-clojure?*

  ""

  false)



(def ^:dynamic *refresh-clojure?*

  ""

  true)



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
  nil)



(defmacro refresh-clojure*

  ""

  []

  (refresh-clojure))
  

;;;;;;;;;; Detecting the current compilation platform


(defn compile-cljs?

  ""

  []

  (boolean (and (not *force-clojure?*)
                (base-cljs?))))



(defmacro compile-cljs?*

  ""

  []

  (compile-cljs?))



(defn compile-clojure?

  ""

  []

  (boolean (or *force-clojure?*
               (base-clojure?))))



(defmacro compile-clojure?*

  ""

  []

  (compile-clojure?))
