(ns
  ^{:doc 
  
  "Launcher for the SimRunner application."
  
    :author "Frank Mosebach"}
  fm.simrunner.main
  (:require
    [fm.simrunner.app :as app])
  (:import 
    (java.io File)))

(defn run [& {:keys [stand-alone? working-directory simtask-name]}]
  (let [stand-alone?      (boolean stand-alone?)
        working-directory (-> working-directory
                              (or ".")
                              File.
                              .getAbsolutePath)
        simtask-name      (str (or simtask-name "simtask"))]
    (app/start {:working-directory working-directory 
                :stand-alone?      stand-alone?
                :simtask-name      simtask-name})))

(defn -main [& [working-directory]]
  (run :working-directory working-directory :stand-alone? true))

