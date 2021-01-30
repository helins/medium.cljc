(ns helins.medium-test

  ""

  {:author "Adam Helinski"}

  (:require [clojure.test  :as t]
            [helins.medium :as medium])
  #?(:cljs (:require-macros [helins.medium-test])))  ;; Just in case. In prior commits, this was ultimately throwing.



;;;;;;;;;;

#?(:clj (println :clojure-test)
   :cljs (println :cljs-test))

;;;;;;;;;;


#?(:clj

(t/deftest cljs-compiler

  (t/is (nil? (medium/cljs-compiler))
        "When compiling true Clojure, the CLJS compiler is nil")))



(t/deftest cljs-compiler-optimization

  (let [x (medium/cljs-compiler-optimization*)]
    (t/is #?(:clj  (nil? x)
             :cljs (some? x))
          "CLJS compiler optimization only exist when compiling CLJS")

    #?(:clj (t/is (= (medium/cljs-compiler-optimization)
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
                     (medium/target-init))
                  "Macro returns the same result as the function"))))
