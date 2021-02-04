(ns helins.medium.co-load

  ""

  {:author "Adam Helinski"}

  (:require [clojure.tools.namespace.dir]
            [clojure.tools.namespace.file]
            [clojure.tools.namespace.reload]
            [clojure.tools.namespace.track]
            [hawk.core                       :as hawk]
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




(defn- -reload

  ;;

  [tracker]

  (when-some [nspace+ (not-empty (vec (tracker :clojure.tools.namespace.track/load)))]
    (log/info (format "Reloading: %s"
                      nspace+)))
  (let [tracker-2 (clojure.tools.namespace.reload/track-reload tracker)]
    (when-some [err (tracker-2 :clojure.tools.namespace.reload/error)]
      (log/fatal err
                 (format "Error while reloading Clojure namespace: %s"
                         (tracker-2 :clojure.tools.namespace.reload/error-ns)))
      tracker)
    tracker-2))



(defn reload-all!

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
                              (clojure.tools.namespace.dir/scan-dirs path+)
                              -reload))))))
      :tracker
      deref)
  nil)



(defn watch!

  ""

  [path+]

  (let [path-2+     (into #{}
                          (map #(.getCanonicalPath (File. %)))
                          path+)
        [state-old
         state-new] (swap-vals! -*state
                                (fn [state]
                                  (if (= path-2+
                                         (state :path+))
                                    state
                                    (if (seq path-2+)
                                      (-> (assoc state
                                                 :path+   path-2+
                                                 :watcher (delay
                                                            (hawk/watch! [{:filter  -hawk-clojure-file?
                                                                           :handler -hawk-handler
                                                                           :paths   path-2+}])))
                                          (update :tracker
                                                  (fn [tracker]
                                                    (delay
                                                      (-> @tracker
                                                          (clojure.tools.namespace.dir/scan-dirs (or path+
                                                                                                     []))
                                                          -reload)))))
                                      (-state)))))
        watcher-old (state-old :watcher)
        watcher-new (state-new :watcher)]
    (when-not (identical? watcher-new
                          watcher-old)
      (log/info (format "Setting watch for: %s"
                        path+))
      (-> state-new
          :tracker
          deref)
      (some-> watcher-old
              (-> deref
                  hawk/stop!))
      (some-> watcher-new
              deref)))
  nil)



(defn reload!

  ""

  []

  (-> (swap! -*state
             update
             :tracker
             (fn [tracker]
               (delay
                 (-reload @tracker))))
      :tracker
      deref)
  nil)



(defn shadow-cljs-hook

  ""

  {:shadow.build/stages #{:compile-prepare
                          :configure}}

  [{:as                build
    :shadow.build/keys [stage]}
   & [param+]]

  (case stage
    :compile-prepare (reload!)
    :configure       (watch! (:path+ param+)))
  build)



(comment

  (shadow-cljs-hook {:shadow.build/stage :configure}
                    {:path+ []})

  (shadow-cljs-hook {:shadow.build/stage :configure}
                    {:path+ ["src/dev"]})

  (shadow-cljs-hook {:shadow.build/stage :compile-prepare})



  (-> -*state
      deref
      :tracker
      deref
      :clojure.tools.namespace.track/unload)

  )



