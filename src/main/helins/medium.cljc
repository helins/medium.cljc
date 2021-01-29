(ns helins.medium

  ""

  {:author "Adam Helinski"}

  #?(:clj  (:require [cljs.env]))
  #?(:cljs (:require-macros [helins.medium :refer [base-cljs?
                                                   base-clojure?
                                                   cljs-level
                                                   cljs-level-raw]])))


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

  (when-some [level-raw (cljs-level-raw)]
    (if (identical? level-raw
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
