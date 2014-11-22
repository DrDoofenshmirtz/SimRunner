(ns
  ^{:doc 
  
  "Performing Actions on a SimRunner Application."
  
    :author "Frank Mosebach"}
  fm.simrunner.actions
  (:require 
    [fm.simrunner (config :as cfg)
                  (model :as mod)
                  (exec :as exc)]
    [fm.simrunner.gui (core :as gui)
                      (rendering :as rdg)])
  (:import 
    (java.io File)
    (javax.swing JOptionPane)))

(defn- run-task [{worker :worker :as app} task & args]
  (send-off worker 
            (fn [_ &]
              (try
                (apply task app args)
                nil
                (catch Exception task-failed
                  (.printStackTrace task-failed)
                  nil)))))

(defmulti on-action {:private true} (fn [action & _] action))

(defn- read-config-file [app widget file]
  (try
    (cfg/read-config-file file)
    (catch Exception read-error
      (gui/gui-do
        (JOptionPane/showMessageDialog (:widget widget) 
                                       "Failed to read config file!" 
                                       "Open Config" 
                                       JOptionPane/ERROR_MESSAGE)
        (rdg/unlock! app))
      nil)))

(defn- open-config [{app-state :state :as app} widget file]
  (when-let [config (read-config-file app widget file)]
    (if (cfg/complete? config)
      (do
        (swap! app-state mod/apply-config config file)
        (rdg/unlock! app))
      (gui/gui-do
        (JOptionPane/showMessageDialog (:widget widget) 
                                       "The selected config file is not valid." 
                                       "Open Config" 
                                       JOptionPane/ERROR_MESSAGE)
        (rdg/unlock! app)))))

(defmethod on-action :open-config [_ app & [widget]]
  (if-let [file (gui/choose-file (:widget widget) :title "Open Config")]
    (run-task app open-config widget file)
    (rdg/unlock! app)))  

(defn- write-config-file [app widget config file]
  (try
    (cfg/write-config-file config file)
    (catch Exception read-error
      (gui/gui-do
        (JOptionPane/showMessageDialog (:widget widget) 
                                       "Failed to read config file!" 
                                       "Open Config" 
                                       JOptionPane/ERROR_MESSAGE)
        (rdg/unlock! app))
      nil)))

(defn- save-config [{app-state :state :as app} widget file]
  (let [config (-> @app-state :model :config)
        config (write-config-file app widget config file)]
    (when config    
      (swap! app-state mod/apply-config config file)
      (rdg/unlock! app))))

(defmethod on-action :save-config [_ app & [widget]]
  (if-let [file (-> app :state deref :model :file)] 
    (run-task app save-config widget file)
    (on-action :save-config-as app widget)))

(defmethod on-action :save-config-as [_ app & [widget]]
  (if-let [file (gui/choose-file (:widget widget) :title "Save Config As")]
    (run-task app save-config widget file)
    (rdg/unlock! app)))  

(defn- exec-args [{:keys [config state]}]
  (let [{:keys [ui model]} @state
        working-dir (:working-directory config)
        config-file (:file model)
        output-file (-> ui :model :values :output-file)]
    (when (and (:valid? model) config-file output-file)
      [(.getAbsolutePath (File. working-dir "bin/simrunner"))
       (.getAbsolutePath config-file)
       (.getAbsolutePath output-file)])))

(defn- console-logger [app]
  (fn [source]
    (exc/drain-lines source (fn [line]
                              (rdg/log-messages! app line)))))

(defn- run-simulation [app widget]
  (if-let [[_ config-file output-file :as args] (exec-args app)]
    (do
      (rdg/log-messages! app 
                         "Starting simulation run."
                         (format "Config file: '%s'" config-file)
                         (format "Ouput file : '%s'" output-file))
      (try
        (exc/wait-for (exc/drain-outputs (apply exc/exec args) 
                                         :out> (console-logger app)
                                         :err> (console-logger app)))
        (rdg/log-messages! app "Simulation run terminated.")
        (rdg/unlock! app)
        (catch Exception exec-error
          (gui/gui-do
            (JOptionPane/showMessageDialog (:widget widget) 
                                           "The simulation run failed." 
                                           "Run Simulation" 
                                           JOptionPane/ERROR_MESSAGE)
            (rdg/unlock! app)))))
    (gui/gui-do
      (JOptionPane/showMessageDialog (:widget widget) 
                                     "The simulation config is not valid." 
                                     "Run Simulation" 
                                     JOptionPane/WARNING_MESSAGE)
      (rdg/unlock! app))))

(defmethod on-action :run-simulation [_ app & [widget]]
  (run-task app run-simulation widget))

(defn- select-input-file [{app-state :state :as app} widget]
  (if-let [file (gui/choose-file (:widget widget) 
                                 :title "Select Input File")]
    (do
      (swap! app-state mod/update-value :input-file file)
      (rdg/unlock! app))
    (rdg/unlock! app)))

(defmethod on-action :select-input-file [_ app & [widget]]
  (select-input-file app widget))

(defn- select-output-file [{app-state :state :as app} widget]
  (if-let [file (gui/choose-file (:widget widget) 
                                 :title "Select Output File")]
    (do
      (swap! app-state mod/update-value :output-file file)
      (rdg/unlock! app))
    (rdg/unlock! app)))

(defmethod on-action :select-output-file [_ app & [widget]]
  (select-output-file app widget))

(defmethod on-action :default [action app & args]
  (println (format "on-action{action: %s args: %s}" action args))
  (rdg/unlock! app))

(defn dispatch [action {app-state :state :as app} args]
  (when-not (-> @app-state :ui :locked?)
    (rdg/lock! app)
    (apply on-action action app args)))

