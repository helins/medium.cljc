(ns helins.medium.dev-2

  ""

  {:author "Adam Helinski"}

  (:require [helins.medium :as medium]))


;;;;;;;;;;


(medium/refresh-clojure*)


#?(:clj (println :reload (str *ns*)))
