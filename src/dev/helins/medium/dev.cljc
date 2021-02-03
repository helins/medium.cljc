(ns helins.medium.dev

  ""

  {:author "Adam Helinski"}

  (:require [helins.medium       :as medium]
            [helins.medium.dev-2]))


;;;;;;;;;;


(medium/next-reload-cycle*)
(medium/co-load*)


#?(:clj (println :reload (str *ns*)))



(defn on-load

  "Gets called once from Clojure at initialization and everytime CLJS reloads."

  []

  )



(comment
  

  )
