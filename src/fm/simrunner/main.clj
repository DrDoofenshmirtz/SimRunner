(ns
  ^{:doc 
  
  "Launcher for the SimRunner application."
  
    :author "Frank Mosebach"}
  fm.simrunner.main
  (:require
    [fm.simrunner.app :as app])
  (:import 
    (java.io File)))

(defn run [& {:keys [stand-alone? working-directory] :or {stand-alone? false}}]
  (let [working-directory (-> (or ".")
                              File.
                              .getAbsolutePath)]
    (app/start {:working-directory working-directory 
                :stand-alone?      stand-alone?})))

(defn -main [& [working-directory]]
  (run :working-directory working-directory :stand-alone? true))

