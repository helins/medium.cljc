(ns helins.medium-test

  ""

  {:author "Adam Helinski"}

  (:require [clojure.test  :as t]
            [helins.medium :as medium])
  #?(:cljs (:require-macros [helins.medium-test])))  ;; Just in case. In prior commits, this was ultimately throwing.


;;;;;;;;;;


(t/deftest cljs-optimization

  (let [x (medium/cljs-optimization*)]
    (t/is #?(:clj  (nil? x)
             :cljs (some? x))
          "CLJS compiler optimization only exist when compiling CLJS")

    #?(:clj (t/is (= medium/cljs-optimization
                     x)
                  "Macro returns the same result as the function"))))



(t/deftest target

  (let [x (medium/target*)]
    (t/is #?(:clj  (= :clojure
                      x)
             :cljs (#{:cljs/dev
                      :cljs/release} x))
          "Current target is set accordingly")

    #?(:clj (t/is (= x
                     (medium/target nil))
                  "Macro returns the same result as the function"))))



(t/deftest target-init

  (let [x (medium/target-init*)]
    (t/is #?(:clj  (= :clojure
                      x)
             :cljs (#{:cljs/dev
                      :cljs/release} x))
          "Initial target is set accordingly")

    #?(:clj (t/is (= x
                     medium/target-init)
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



(medium/expand*
  (let [x 1]

    `(def expand-test-var

       (+ ~x
          expand-test-value))))



(t/deftest expand*

  (t/is (= (+ 1
              expand-test-value)
           expand-test-var)
        "Var was defined during expansion"))



#?(:clj (def *a (atom false)))


(medium/when-compiling*
  (reset! *a
          true))



(t/deftest when-compiling*

  (t/is (medium/expand* @*a)))


;;;;;;;;;;


(t/deftest load-edn*


  (t/is (= {:a ['b "c" #{4}]}
           (medium/load-edn* "./resources/load.edn"))))
