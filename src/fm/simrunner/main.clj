(ns
  ^{:doc 
  
  "Launcher for the SimRunner application."
  
    :author "Frank Mosebach"}
  fm.simrunner.main
  (:require
    [fm.simrunner.app :as app])
  (:import 
    (java.io File)))

(def ^{:private true :const true} prop-key-prefix "fm.simrunner.")

(def ^{:private true :const true} config-keys #{:app-title
                                                :working-directory 
                                                :simtask-name})

(defn run [& {:keys [stand-alone? app-title working-directory simtask-name]}]
  (let [stand-alone?      (boolean stand-alone?)
        app-title         (str (or app-title "SimRunner (c) 2014 DEINC"))
        working-directory (-> working-directory
                              (or ".")
                              File.
                              .getAbsolutePath)
        simtask-name      (str (or simtask-name "simtask"))]
    (app/start {:app-title         app-title
                :working-directory working-directory 
                :stand-alone?      stand-alone?
                :simtask-name      simtask-name})))

(defn- system-property [config-key]
  (let [value (-> (str prop-key-prefix (name config-key))
                  System/getProperty
                  str
                  .trim)]
    (when-not (.isEmpty value)
      value)))

(defn -main [& _]
  (let [args (reduce (fn [args config-key]
                       (conj args config-key (system-property config-key)))
                     [:stand-alone? true]
                     config-keys)]
    (apply run args)))

