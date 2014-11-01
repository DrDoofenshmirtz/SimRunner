(ns
  ^{:doc 
  
  "Launches the SimRunner application."
  
    :author "Frank Mosebach"}
  fm.simrunner.main
  (:gen-class
    :name fm.simrunner.Main
    :main true)
  (:require
    [fm.simrunner.gui (core :as gui) 
                      (view :as view) 
                      (wiring :as wiring)]))

(defn- on-event [& args]
  (println (format "-- on-event: %s" args)))

(defn run [& args]
  @(gui/gui-do
    (let [frame (view/simrunner-frame)
          view  (-> frame :contents :simrunner-view)]
      (wiring/wire-up view on-event)
      (-> frame :widget .show))))

(defn -main [& args]
  (apply run args))

