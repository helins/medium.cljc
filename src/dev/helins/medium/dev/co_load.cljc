(ns helins.medium.dev.co-load

  ""

  {:author "Adam Helinski"}

  (:require [helins.medium :as medium])
  #?(:cljs (:require-macros [helins.medium.dev.co-load])))


;;;;;;;;;;


(defmacro current-ns*

  ""

  []

  (medium/co-load &env))
