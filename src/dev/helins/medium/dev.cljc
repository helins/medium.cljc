(ns helins.medium.dev

  ""

  {:author "Adam Helinski"}

  (:require [helins.medium             :as medium]
            [helins.medium.dev.co-load :as medium.dev.co-load]
            [helins.medium.dev-2]
            [helins.medium.dev-3       :as medium.dev-3]))


;;;;;;;;;;


(medium.dev.co-load/current-ns* medium.dev-3/foo)


#?(:clj (println :reload (str *ns*)))



(defn on-load

  "Gets called once from Clojure at initialization and everytime CLJS reloads."

  []

  )



(comment
  

  )
