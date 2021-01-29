(ns helins.medium-test

  ""

  {:author "Adam Helinski"}

  (:require [clojure.test  :as t]
            [helins.medium :as medium]))


;;;;;;;;;;


(t/deftest cljs-level-raw

  (let [x (medium/cljs-level-raw)]
    (t/is #?(:clj  (nil? x)
             :cljs (some? x)))))



(t/deftest cljs-level

  (let [x (medium/cljs-level)]
    (t/is #?(:clj  (nil? x)
             :cljs (#{:dev
                      :release} x)))))


;;;;;;;;;;


(t/deftest base-cljs?

  (t/is (= #?(:clj  false
              :cljs true)
           (medium/base-cljs?))))



(t/deftest base-clojure?

  (t/is (= #?(:clj  true
              :cljs false)
           (medium/base-clojure?))))
