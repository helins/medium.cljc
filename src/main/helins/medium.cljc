(ns helins.medium

  ""

  {:author "Adam Helinski"}

  #?(:clj  (:require [cljs.env]))
  #?(:cljs (:require-macros [helins.medium :refer [base-cljs?
                                                   base-clojure?
                                                   cljs-level
                                                   cljs-level-raw]])))


;;;;;;;;;; CLJS compilation levels


(defmacro cljs-level-raw

  ""

  []

  (some-> cljs.env/*compiler*
          deref
          (get-in [:options
                   :optimizations])))



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

  (some? (cljs-level-raw)))




(defmacro base-clojure?

  ""

  []

  (nil? (cljs-level-raw)))
