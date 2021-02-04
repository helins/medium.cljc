(ns helins.medium.dev

  ""

  {:author "Adam Helinski"}

  (:require [helins.medium             :as medium]
            [helins.medium.dev.co-load :as medium.dev.co-load]
            [helins.medium.dev-2]))


;;;;;;;;;;


(medium.dev.co-load/current-ns*)


#?(:clj (println :reload (str *ns*)))



(defn on-load

  "Gets called once from Clojure at initialization and everytime CLJS reloads."

  []

  )



(comment
  

  )
