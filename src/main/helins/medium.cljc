(ns helins.medium

  ""

  {:author "Adam Helinski"}

  #?(:clj (:require [clojure.edn]
                    [clojure.string]
                    [clojure.walk]))
  #?(:cljs (:require-macros [helins.medium :refer [-init-as-cljs*
                                                   cljs-optimization*
                                                   expand*
                                                   load-edn*
                                                   load-string*
                                                   target*
                                                   target-init*
                                                   touch-recur*
                                                   when-compiling*
                                                   when-target*]]))
  #?(:clj (:import java.io.File)))


;;;;;;;;;; Extracting information from the Clojurescript compiler


#?(:clj (def cljs-optimization

  ""

  nil))



(defmacro cljs-optimization*

  ""

  []

  cljs-optimization)


;;;;;;;;; Detecting the target (at initialization and currently)


#?(:clj (def target-init

  ""

  :clojure))



(defmacro target-init*

  ""

  []

  target-init)



#?(:clj (defn target

  ""

  [env]

  (if (:ns env)
    target-init
    :clojure)))



(defmacro target*

  ""

  []

  (target &env))


;;;;;


(defn cljs?

  ""

  [target]

  (or (= target
         :cljs/dev)
      (= target
         :cljs/release)))


(defn clojure?

  ""

  [target]

  (= target
     :clojure))


;; <!> Important <!>
;;
;; Detects if initially compiling for CLJS. The trick is executing Clojure code only when this namespace
;; is required as a CLJS file.


(defmacro ^:no:doc -init-as-cljs*

  ;;

  []

  (let [optimization (get-in @@(requiring-resolve 'cljs.env/*compiler*)
                             [:options
                              :optimizations])]
    (alter-var-root #'cljs-optimization
                    (constantly optimization))
    (alter-var-root #'target-init
                    (constantly
                      (case (&env :shadow.build/mode)
                        :dev     :cljs/dev
                        :release :cljs/release
                        (if (identical? optimization
                                        :none)
                          :cljs/dev
                          :cljs/release)))))
  nil)



#?(:cljs (-init-as-cljs*))


;;;;;;;;;; Macros simplify outputting code depending on target


#?(:clj (defn not-cljs-release

  ""

  [env form]

  (when (identical? (target env)
                    :cljs/release)
    (throw (ex-info (str "Call forbidden in CLJS advanced build: "
                         form)
                    {:medium/forbidden (symbol (resolve (first form)))
                     :medium/form      form})))))



#?(:clj (defn- -when-requested-target

  ""

  [target-request target form+]

  (when (if (coll? target-request)
          (some #(identical? %
                             target)
                target-request)
          (identical? target
                      target-request))
    `(do ~@form+))))



(defmacro when-target*

  ""

  [target-request & form+]

  (-when-requested-target target-request
                          (target &env)
                          form+))



(defmacro when-target-init*

  ""

  [target-request & form+]

  (-when-requested-target target-request
                          target-init
                          form+))


;;;;;;;;;; Testing file extensions


(defn file-cljc?

  ""

  [filename]

  (clojure.string/ends-with? filename
                             ".cljc"))



(defn file-cljs?

  ""

  [filename]

  (or (file-cljc? filename)
      (clojure.string/ends-with? filename
                                 ".cljs")))



(defn file-clojure?

  ""

  [filename]

  (or (file-cljc? filename)
      (clojure.string/ends-with? filename
                                 ".clj")))


;;;;;;;;;; Touching files (mainly for forcing recompilation in watching compilers)


#?(:clj (defn touch-recur

  ""

  ([path]

   (touch-recur path
                nil))


  ([path pred]

   (let [now    (System/currentTimeMillis)
         pred-2 (or pred
                    (fn [_]
                      true))]
     (into []
           (comp (map (fn [^File file]
                        (let [path (.getCanonicalPath file)]
                          (when (pred-2 path)
                            (.setLastModified file
                                              now)
                            path))))
                 (filter some?))
           (file-seq (File. path)))))))



(defmacro touch-recur*

  ""

  ([path]

   `(touch-recur* ~path
                  nil))


  ([path pred]

   (touch-recur path
                (eval pred))))


;;;;;;;;;; Loading Clojure alonside CLJS


#?(:clj (def ^:private -*co-loaded

  ;;

  (atom #{})))



#?(:clj (defn co-loaded

  ""

  []

  @-*co-loaded))



#?(:clj (defn detect-dep+

  ""

  ([form]

   (detect-dep+ form
                *ns*))


  ([form nspace]

   (detect-dep+ form
                nspace
                #{}))


  ([form nspace acc]

   (if (coll? form)
     (reduce (fn [acc-2 form-2]
               (detect-dep+ form-2
                            nspace
                            acc-2))
             acc
             form)
     (if-let [var-resolved (and (symbol? form)
                                (ns-resolve nspace
                                            form))]
       (conj acc
             (namespace (symbol var-resolved)))
       acc)))))
          


#?(:clj (defn co-load

  ""

  [env]

  (boolean
    (when-not (identical? (target env)
                          :clojure)
      (let [nspace-2 (ns-name *ns*)]
        (when-not (-> (swap-vals! -*co-loaded
                                  conj
                                  nspace-2)
                      first
                      (contains? nspace-2))
          (require nspace-2
                   :reload)
          true))))))



#?(:clj (defn compiler-hook

  ""

  {:shadow.build/stage :compile-finish}

  [& [state]]

  (when (identical? target-init
                    :cljs/dev)
    (reset! -*co-loaded
            #{}))
  state))


;;;;;;;;;; Anonymous macros


#?(:clj (defn- -resolve-target

  ;;

  [target form+]

  (clojure.walk/postwalk #(if (symbol? %)
                            (case (name %)
                              "&target"      target
                              "&target-init" target-init
                              %)
                            %)
                         form+)))



(defmacro expand*

  ""

  [& clojure-form+]

  (let [target-now (target &env)
        form       `(do
                      ~@(-resolve-target target-now
                                         clojure-form+))]
    (if (identical? target-now
                    :clojure)
      `(eval ~form)
      (do
        (co-load &env)
        (eval form)))))



(defmacro when-compiling*

  ""

  [& clojure-form+]

  (let [target-now (target &env)
        form       `(do
                      ~@(-resolve-target target-now
                                         clojure-form+)
                      nil)]
    (if (identical? target-now
                    :clojure)
      form
      (do
        (co-load &env)
        (eval form)
        nil))))


;;;;;;;;;; Loading and expanding content from files


#?(:clj (defn load-edn

  ""

  [path]

  (clojure.edn/read-string (slurp path))))



(defmacro load-edn*

  ""

  [path]

  `(quote ~(load-edn path)))



(defmacro load-string*

  ""

  [path]

  (slurp path))
