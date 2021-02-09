(ns helins.medium.co-load

  ""

  {:author "Adam Helinski"}

  (:require [clojure.set]
            [clojure.tools.namespace.dir]
            [clojure.tools.namespace.file]
            [clojure.tools.namespace.reload]
            [clojure.tools.namespace.track]
            [hawk.core                       :as hawk]
            [helins.medium                   :as medium]
            [taoensso.timbre                 :as log])
  (:import java.io.File))


;;;;;;;;;;


(defn- -state

  ""

  []

  {:tracker (delay
              (clojure.tools.namespace.track/tracker))})



(def ^:private -*state

  ;;

  (atom (-state)))



(defn compile-cycle

  ""

  []

  (-> -*state
      deref
      :compile-cycle))



(defn- -hawk-handler

  ""

  [_hawk-ctx {:keys [file
                     kind]}]

  (swap! -*state
         update
         :tracker
         (fn [tracker]
           (delay
             ((if (identical? kind
                             :delete)
                clojure.tools.namespace.file/remove-files
                clojure.tools.namespace.file/add-files)
              @tracker
              [file]))))
  nil)



(defn- -hawk-clojure-file?

  ;;

  [_ctx {:keys [file]}]

  (clojure.tools.namespace.file/clojure-file? file))



(defn- -exec-plugin-hook+

  ;;

  [stage plugin+ param+]

  (doseq [[plugin-sym
           plugin-config] plugin+]
    (if-some [var-plugin (try
                           (requiring-resolve plugin-sym)
                           (catch Throwable e
                             (log/error e
                                        (format "While requiring and resolving plugin hook: %s for %s"
                                                plugin-sym
                                                stage))
                             nil))]
      (let [meta-plugin (meta var-plugin)]
        (when (or (identical? (:shadow.build/stage meta-plugin)
                              stage)
                  (contains? (:shadow.build/stages meta-plugin)
                             stage))
          (try
            (@var-plugin (-> (merge param+
                                    plugin-config)
                             (assoc :shadow.build/stage
                                    stage)))
            (catch Throwable e
              (log/error e
                         (format "While executing plugin  hook: %s for %s"
                                 plugin-sym
                                 stage))))))
      (log/error (format "Unable to resolve: %s"
                         plugin-sym))))
  nil)



(defn- -reload

  ;;

  [tracker stage plugin+]

  (let [load+    (into #{}
                       (tracker :clojure.tools.namespace.track/load))
        unload+  (into #{}
                       (tracker :clojure.tools.namespace.track/unload))
        remove+  (clojure.set/difference unload+
                                         load+)]
    (when (seq remove+)
      (log/info (format "Will unload: %s"
                        remove+)))
    (when (seq load+)
      (log/info (format "Will load: %s"
                        load+)))
    (-exec-plugin-hook+ stage
                        plugin+
                        {:medium.co-load/load+   load+
                         :medium.co-load/stage   stage
                         :medium.co-load/unload+ remove+}))
  (let [tracker-2 (clojure.tools.namespace.reload/track-reload tracker)]
    (when-some [err (tracker-2 :clojure.tools.namespace.reload/error)]
      (log/fatal err
                 (format "Error while reloading Clojure namespace: %s during %s"
                         (tracker-2 :clojure.tools.namespace.reload/error-ns)
                         stage))
      tracker)
    tracker-2))




(defn clear!

  ""

  []

  (-> (swap! -*state
             (fn [{:as   state
                   :keys [path+
                          tracker]}]
               (cond->
                 state
                 (seq path+)
                 (assoc :tracker
                        (delay
                          (-> @tracker
                              (dissoc :clojure.tools.namespace.dir/time)
                              (clojure.tools.namespace.dir/scan-dirs path+)))))))
      :tracker
      deref)
  nil)



(defn configure

  ""

  [plugin+]

  (let [path+       (not-empty (into #{}
                                     (comp (map second)
                                           (mapcat :medium.co-load/path+)
                                           (map #(.getCanonicalPath (File. %))))
                                     plugin+))
        [state-old
         state-new] (swap-vals! -*state
                                (fn [state]
                                  (if (= path+
                                         (state :path+))
                                    state
                                    (if path+
                                      (-> (assoc state
                                                 :path+   path+
                                                 :watcher (delay
                                                            (hawk/watch! [{:filter  -hawk-clojure-file?
                                                                           :handler -hawk-handler
                                                                           :paths   path+}])))
                                          (update :tracker
                                                  (fn [tracker]
                                                    (delay
                                                      (-> @tracker
                                                          (clojure.tools.namespace.dir/scan-dirs path+)
                                                          (-reload :configure
                                                                   plugin+))))))
                                      (-state)))))
        watcher-old (state-old :watcher)
        watcher-new (state-new :watcher)]
    (if (identical? watcher-new
                    watcher-old)
      (-exec-plugin-hook+ :configure
                          plugin+
                          nil)
      (do
        (if path+
          (log/info (format "Watching: %s"
                            path+))
          (log/warn "Watching nothing: no path specified"))
        (-> state-new
            :tracker
            deref)
        (some-> watcher-old
                (-> deref
                    hawk/stop!))
        (some-> watcher-new
                deref))))
  nil)



(defn compile-prepare

  ""

  [plugin+]

  (-> (swap! -*state
             update
             :tracker
             (fn [tracker]
               (delay
                 (-reload @tracker
                          :compile-prepare
                          plugin+))))
      :tracker
      deref)
  nil)



(defn shadow-cljs-hook

  ""

  {:shadow.build/stages #{:compile-finish
                          :compile-prepare
                          :configure}}

  [{:as                    build
    :shadow.build/keys     [stage]
    :shadow.build.api/keys [compile-cycle]}
   plugin+]

  (try
    (some->> compile-cycle
             (swap! -*state
                    assoc
                    :compile-cycle))
    (case stage
      :compile-finish  (-exec-plugin-hook+ stage
                                           plugin+
                                           nil)
      :compile-prepare (compile-prepare plugin+)
      :configure       (configure plugin+))
    (catch Throwable e
      (log/fatal e
                 (format "During compilation stage %s"
                         stage))))
  build)




(defn ^:no-doc -plugin-test

  ;;

  {:shadow.build/stages #{:compile-finish
                          :configure}}

  [x]

  (println :plugin-hook x))




(comment


  (-> -*state
      deref
      :tracker
      deref
      :clojure.tools.namespace.track/unload)


  )
