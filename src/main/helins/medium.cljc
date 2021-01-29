(ns helins.medium

  ""

  {:author "Adam Helinski"}

  #?(:clj  (:require [cljs.env]
                     [clojure.tools.namespace.repl]))
  #?(:cljs (:require-macros [helins.medium :refer [base-cljs?
                                                   base-clojure?
                                                   cljs-level
                                                   cljs-level-raw
                                                   compile-cljs?
                                                   compile-clojure?
                                                   force-clojure
                                                   force-clojure?
                                                   refresh-clojure
                                                   ]])))


(clojure.tools.namespace.repl/disable-reload!)


;;;;;;;;;; Miscellaneous


#?(:clj 
   
(defn cljs-compiler

  ""

  []

  (some-> cljs.env/*compiler*
          deref)))


;;;;;;;;;; CLJS compilation levels


(defmacro cljs-level-raw

  ""

  []

  (get-in (cljs-compiler)
          [:options
           :optimizations]))



(defmacro cljs-level

  ""

  []

  `(when-some [level-raw# (cljs-level-raw)]
     (if (identical? level-raw#
                     :advanced)
       :release
       :dev)))


;;;;;;;;;; Detecting the base platform


(defmacro base-cljs?

  ""

  []

  (some? (cljs-compiler)))



(defmacro base-clojure?

  ""

  []

  (nil? (cljs-compiler)))


;;;;;;;;;; Refreshing Clojure files from CLJS


#?(:clj

(def ^:dynamic *force-clojure?*

  ""

  false))



#?(:clj

(def ^:dynamic *refresh-clojure?*

  ""

  true))



(defmacro refresh-clojure

  ""

  ;; TODO. Disable when already in Clojure?

  []

  (when *refresh-clojure?*
    (println :REFRESHING)
    (binding [*force-clojure?*   true
              *refresh-clojure?* false
              clojure.core/*e    nil]
      (clojure.tools.namespace.repl/refresh)
      (some-> clojure.core/*e
              throw)))
  nil)
  

;;;;;;;;;; Detecting the current compilation platform


(defmacro compile-cljs?

  ""

  []

  (boolean (and (eval `(base-cljs?))
                not *force-clojure?*)))



(defmacro compile-clojure?

  ""

  []

  (boolean (or (eval `(base-clojure?))
               *force-clojure?*)))
