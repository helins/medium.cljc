(ns helins.medium.dev-2

  ""

  {:author "Adam Helinski"}

  (:require [helins.medium :as medium]))


;;;;;;;;;;


(medium/co-load*)


#?(:clj (println :reload (str *ns*)))
