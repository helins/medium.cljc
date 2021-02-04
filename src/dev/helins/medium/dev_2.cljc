(ns helins.medium.dev-2

  ""

  {:author "Adam Helinski"}

  (:require [helins.medium.dev.co-load :as medium.dev.co-load]))


;;;;;;;;;;


(medium.dev.co-load/current-ns*)


#?(:clj (println :reload (str *ns*)))
