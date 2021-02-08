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

  (doseq [[plugin-id
           plugin-config] plugin+]
    (when-some [sym-f (plugin-config stage)]
      (if-some [var-f (try
                        (requiring-resolve sym-f)
                        (catch Throwable e
                          (log/error e
                                     (format "While requiring and resolving plugin hook: %s for %s"
                                             sym-f
                                             stage))
                          nil))]
        (try
          (@var-f (assoc param+
                         :medium.co-load/stage         stage
                         :medium.co-load.plugin/id     plugin-id
                         :medium.co-load.plugin/config plugin-config))
          (catch Throwable e
            (log/error e
                       (format "While executing pluing hook: %s for %s"
                               sym-f
                               stage))))
        (log/error (format "Unable to resolve: %s"
                           sym-f)))))
  nil)



(defn- -reload

  ;;

  [tracker stage plugin+]

  (let [unload+  (into #{}
                       (tracker :clojure.tools.namespace.track/unload))
        reload+  (into #{}
                      (tracker :clojure.tools.namespace.track/load))
        remove+  (clojure.set/difference unload+
                                         reload+)]
    (when (seq remove+)
      (log/info (format "Will unload: %s"
                        remove+)))
    (when (seq reload+)
      (log/info (format "Will reload: %s"
                        reload+)))
    (-exec-plugin-hook+ stage
                        plugin+
                        {:medium.co-load/remove+ remove+
                         :medium.co-load/stage   stage}))
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
                                           (mapcat :path+)
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
    (when-not (identical? watcher-new
                          watcher-old)
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
              deref)))
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




(defn ^:no-doc -plugin-hook-test

  ;;

  [x]

  (println :plugin-hook x))




(comment

  (shadow-cljs-hook {:shadow.build/stage :configure}
                    {:test {
                            :path+ ["src/dev"]}})

  (shadow-cljs-hook {:shadow.build/stage :configure}
                    nil)

  (shadow-cljs-hook {:shadow.build/stage :compile-prepare}
                    nil)



  (-> -*state
      deref
      :tracker
      deref
      :clojure.tools.namespace.track/unload)

  )



