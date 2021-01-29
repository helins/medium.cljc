(ns helins.medium.dev

  ""

  {:author "Adam Helinski"}

  (:require [helins.medium :as medium]))


;;;;;;;;;;



(defn on-load

  "Gets called once from Clojure at initialization and everytime CLJS reloads."

  []

  )



(println :REF-1 medium/*refresh-clojure?*)

(medium/refresh-clojure)

(println :REF-2 medium/*refresh-clojure?*)

(comment
  

  )
