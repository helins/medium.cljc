(ns helins.medium.dev.co-load

  ""

  {:author "Adam Helinski"}

  (:require [helins.medium :as medium])
  #?(:cljs (:require-macros [helins.medium.dev.co-load])))


;;;;;;;;;;


(defmacro current-ns*

  ""


  ([]

   `(current-ns* nil))


  ([form]

   (println :co-loading (medium/co-load &env
                                        form))))
