(ns helins.medium

  ""

  {:author "Adam Helinski"}

  #?(:clj (:require [clojure.edn]
                    [clojure.string]
                    [clojure.walk]))
  #?(:cljs (:require-macros [helins.medium :refer [-init-as-cljs*
                                                   cljs-optimization*
                                                   co-load*
                                                   expand*
                                                   load-edn*
                                                   load-string*
                                                   next-reload-cycle*
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


;;;;;;;;;; Macros simplify output code depending on target


#?(:clj (defn not-cljs-release

  ""

  [env form]

  (when (identical? (target env)
                    :cljs/release)
    (throw (ex-info (str "Call forbidden in CLJS advanced build: "
                         form)
                    {:medium/forbidden (symbol (resolve (first form)))
                     :medium/form      form})))))



(defn- -when-requested-target

  ""

  [target-request target form+]

  (when (if (coll? target-request)
          (some #(identical? %
                             target)
                target-request)
          (identical? target
                      target-request))
    `(do ~@form+)))



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


;;;;;;;;;; File extensions


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


;;;;;;;;;; Refreshing CLJS files


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
                            (println :path path)
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


;;;;;;;;;; Refreshing Clojure files


#?(:clj (def ^:private -*reloaded

  ;;

  (atom #{})))



#?(:clj (defn co-load

  ""

  ([env]

   (co-load env
            nil))


  ([env nspace]

   (when-not (identical? (target env)
                         :clojure)
     (let [nspace-2 (ns-name (or nspace
                                 *ns*))]
       (when-not (-> (swap-vals! -*reloaded
                                 conj
                                 nspace-2)
                     first
                     (contains? nspace-2))
         (require nspace-2
                  :reload))))
   nil)))



(defmacro co-load*

  ""

  []

  (co-load &env)
  nil)



#?(:clj (defn next-reload-cycle

  ""

  [env]


  (when (identical? (target env)
                    :cljs/dev)
    (reset! -*reloaded
            #{}))))



(defmacro next-reload-cycle*

  ""

  []

  (next-reload-cycle &env)
  nil)


;;;;;;;;;; Anonymous macros


#?(:clj (defn- -form

  ;;

  [target form+]

  `(do ~@(clojure.walk/postwalk #(if (symbol? %)
                                   (case (name %)
                                     "&target"      target
                                     "&target-init" target-init
                                     %)
                                   %)
                                form+))))



(defmacro expand*

  ""

  [& clojure-form+]

  (let [target-now (target &env)
        form       (-form target-now
                          clojure-form+)]
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
        form       (-form target-now
                          clojure-form+)]
    (if (identical? target-now
                    :clojure)
      `(do
         ~form
         nil)
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


;;;;;;;;;; <!> Important <!>
;;;;;;;;;;
;;;;;;;;;; Detects if initially compiling for CLJS. The trick is executing Clojure code only when this namespace
;;;;;;;;;; is required as a CLJS file.


(defmacro ^:no:doc -init-as-cljs*

  ;;

  []

  (let [optimization (get-in @@(requiring-resolve 'cljs.env/*compiler*)
                             [:options
                              :optimizations])]
    (alter-var-root #'cljs-optimization
                    (constantly optimization))
    (alter-var-root #'target-init
                    (constantly (if (identical? optimization
                                                :advanced)
                                  :cljs/release
                                  :cljs/dev))))
  nil)



#?(:cljs (-init-as-cljs*))
