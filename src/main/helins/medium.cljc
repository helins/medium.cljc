(ns helins.medium

  ""

  {:author "Adam Helinski"}

  #?(:clj (:require [clojure.edn]
                    [clojure.string]
                    [clojure.tools.namespace.repl]
                    [clojure.walk]
                    [me.raynes.fs                  :as fs]))
  #?(:cljs (:require-macros [helins.medium :refer [cljs-optimization*
                                                   expand*
                                                   load-edn*
                                                   load-string*
                                                   refresh-cljs*
                                                   refresh-clojure*
                                                   target*
                                                   target-init*
                                                   touch-recur*
                                                   when-compiling*
                                                   when-target*]])))


#?(:clj (clojure.tools.namespace.repl/disable-reload!))


;;;;;;;;;; Flags


#?(:clj (def ^:dynamic *refresh-clojure?*

  ""

  true))


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

   (let [touch (if pred
                 #(when (pred %)
                    (fs/touch %)
                    %)
                 #(do
                    (fs/touch %)
                    %))]
     (if (fs/directory? path)
       (into []
             (comp (mapcat (fn [[root _dir+ file+]]
                             (map #(touch (str root
                                               "/"
                                               %))
                                  file+)))
                   (filter some?))
             (fs/iterate-dir path))
       (when (touch path)
         [path]))))


  ([target path pred]

   (when (#{:cljs/dev
            :clojure} target)
     (touch-recur path
                  pred)))))
             



(defmacro touch-recur*

  ""

  ([path]

   `(touch-recur* ~path
                  nil))


  ([path pred]

   (touch-recur (target &env)
                path
                pred)))


;;;;;


#?(:clj (defn refresh-cljs

  ""

  ([]

   (refresh-cljs))


  ([path]

   (touch-recur (or path
                    "src")
                file-cljs?))


  ([target path]

   (when (#{:cljs/dev
            :clojure} target)
     (refresh-cljs path)))))



(defmacro refresh-cljs*

  ""

  ([]

   `(refresh-cljs* nil))


  ([path]

   (refresh-cljs (target &env)
                 path)))


;;;;;;;;;; Refreshing Clojure files


#?(:clj (defn refresh-clojure

  ""

  ;; TODO. Disable when already in Clojure?

  []

  (when *refresh-clojure?*
    (binding [*refresh-clojure?* false
              clojure.core/*e    nil]
      (clojure.tools.namespace.repl/refresh)
      (some-> clojure.core/*e
              throw)))
  nil))



(defmacro refresh-clojure*

  ""

  []

  (when (cljs? (target &env))
    (refresh-clojure)))


;;;;;;;;;; Anonymous macros


#?(:clj (defn- -prepare-form+

  ;;

  [env form+]

  `(do ~@(clojure.walk/postwalk #(if (symbol? %)
                                   (case (name %)
                                     "&target"      (target env)
                                     "&target-init" (target-init)
                                     %)
                                   %)
                                form+))))



(defmacro expand*

  ""

  [& clojure-form+]

  (when (cljs? (target &env))
    (refresh-clojure))
  (eval (-prepare-form+ &env
                        clojure-form+)))



(defmacro when-compiling*

  ""

  [& clojure-form+]

  (when (cljs? (target &env))
    (refresh-clojure))
  (eval (concat (-prepare-form+ &env
                                clojure-form+)
                [nil])))


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

  `(quote ~(slurp path)))


;;;;;;;;;; <!> Important <!>
;;;;;;;;;;
;;;;;;;;;; Detects if initially compiling for CLJS. The trick is executing Clojure code only when this namespace
;;;;;;;;;; is required as a CLJS file.


#?(:cljs (when-compiling*

(let [optimization (get-in @@(requiring-resolve 'cljs.env/*compiler*)
                           [:options
                            :optimizations])]
  (alter-var-root #'cljs-optimization
                  (constantly optimization))
  (alter-var-root #'target-init
                  (constantly (if (identical? optimization
                                              :advanced)
                                :cljs/release
                                :cljs/dev))))))
