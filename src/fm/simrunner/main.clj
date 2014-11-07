(ns
  ^{:doc 
  
  "Launcher for the SimRunner Application."
  
    :author "Frank Mosebach"}
  fm.simrunner.main
  (:gen-class
    :name fm.simrunner.Main
    :main true)
  (:require
    [fm.simrunner.app :as app])
  (:import 
    (java.io File)))


(defn run [& args]
  (let [[working-directory] args
        working-directory   (-> (or ".")
                                File.
                                .getAbsolutePath)]
    (app/start {:working-directory working-directory})))

(defn -main [& args]
  (apply run args))

