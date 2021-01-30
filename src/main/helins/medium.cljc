(ns helins.medium

  ""

  {:author "Adam Helinski"}

  #?(:clj (:require [clojure.edn]
                    [clojure.string]
                    [clojure.tools.namespace.repl]
                    [clojure.walk]
                    [me.raynes.fs                  :as fs]))
  #?(:cljs (:require-macros [helins.medium :refer [cljs-compiler-optimization*
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


#?(:clj

(def ^:dynamic *refresh-clojure?*

  ""

  true))


;;;;;;;;;; Extracting information from the Clojurescript compiler


#?(:clj


(def ^{:arglist '([])}
     cljs-compiler

  ""

  (if-some [var-cljs-compiler (try
                                (requiring-resolve 'cljs.env/*compiler*)
                                (catch Throwable _e
                                  nil))]
    (fn []
      (some-> @var-cljs-compiler
              deref))
    (fn [] nil))))



#?(:clj

(defn cljs-compiler-optimization

  ""

  []

  (get-in (cljs-compiler)
          [:options
           :optimizations])))



(defmacro cljs-compiler-optimization*

  ""

  []

  (cljs-compiler-optimization))


;;;;;;;;; Detecting the target (at initialization and currently)

#?(:clj

(defn target-init

  ""

  []

  (if-some [level-raw (cljs-compiler-optimization)]
    (if (identical? level-raw
                    :advanced)
      :cljs/release
      :cljs/dev)
    :clojure)))


(defmacro target-init*

  ""

  []

  (target-init))



#?(:clj

(defn target

  ""

  [env]

  (if (:ns env)
    (target-init)
    :clojure)))



(defmacro target*

  ""

  []

  (if (:ns &env)
    (target-init)
    :clojure))


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


(defmacro when-target*

  ""

  [target & form+]

  (let [target-test (helins.medium/target &env)]
    (when (if (coll? target)
            (some #(identical? %
                               target-test)
                  target)
            (identical? target
                        target-test))
      `(do ~@form+))))


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


#?(:clj

(defn touch-recur

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


#?(:clj

(defn refresh-cljs

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


#?(:clj

(defn refresh-clojure

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


#?(:clj


(defn- -prepare-form+

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


;;;;;;;;;;


#?(:clj

(defn load-edn

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
