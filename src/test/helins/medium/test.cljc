;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.medium.test

  ""

  {:author "Adam Helinski"}

  (:require [clojure.test  :as t]
            [helins.medium :as medium]))


;;;;;;;;;;


(def prop-target

  ;;

  (case (medium/expand* (System/getenv "HELINS_MEDIUM_TEST"))
    "clojure"      :clojure
    "cljs-dev"     :cljs/dev
    "cljs-release" :cljs/release))


;;;;;;;;;;


(t/deftest target

  (let [x (medium/target*)]
    (t/is (= prop-target
             x)
          "Current target is set accordingly")

    #?(:clj (t/is (= x
                     (medium/target nil))
                  "Macro returns the same result as the function"))))


;;;;;;;;;;


(t/deftest when-target*

  (t/is (medium/when-target* #?(:clj  :clojure
                                :cljs #{:cljs/dev
                                        :cljs/release})
          true)))


;;;;;;;;;;


(def expand-test-value
     420)



(def *expand-test
     (atom nil))



(medium/expand*
  (let [x 1]
    `(reset! *expand-test
             ~(+ expand-test-value
                 x))))


(t/deftest expand*

  (t/is (= (+ 1
              expand-test-value)
           @*expand-test)
        "Var was defined during expansion"))



#?(:clj (def *a (atom false)))


#?(:clj (def b true))


(medium/when-compiling*
  (reset! *a
          b))



(t/deftest when-compiling*

  (t/is (medium/expand* @*a)))
