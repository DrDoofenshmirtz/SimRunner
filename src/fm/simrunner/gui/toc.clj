(ns
  ^{:doc 
    
  "Creation of a Table of View Contents."
    
    :author "Frank Mosebach"}
   fm.simrunner.gui.toc
   (:require
     [fm.simrunner.gui.core :as gui]))  

(def ^{:private true} buttons-path [:contents :tool-bar :contents])

(def ^{:private true} inputs-path [:contents :config-editor :contents :inputs])

(def ^{:private true} console-path [:contents :console])

(defn- add-buttons [view]
  (let [buttons (get-in view buttons-path)]
    (vary-meta view assoc-in [::toc :buttons] buttons)))

(defn- add-inputs [view]
  (let [inputs (->> (get-in view inputs-path)
                    (map #(vector (-> % meta :id) %))
                    (into {}))]
    (vary-meta view assoc-in [::toc :inputs] inputs)))

(defn- add-console [view]
  (vary-meta view assoc-in [::toc :console] (get-in view console-path)))

(defn- add-widgets [view]
  (vary-meta view assoc-in [::toc :widgets] (gui/widget-seq view)))

(defn with-toc [view]
  (-> view add-buttons add-inputs add-console))

(defn toc [view]
  (-> view meta ::toc))

(defn buttons [view]
  (-> view toc :buttons vals))

(defn inputs [view]
  (-> view toc :inputs vals))

(defn console [view]
  (-> view toc :console))

(defn widgets [view]
  (-> view toc :widgets))

