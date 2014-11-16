(ns
  ^{:doc 
  
  "Performing Actions on a SimRunner Application."
  
    :author "Frank Mosebach"}
  fm.simrunner.actions
  (:require 
    [fm.simrunner.config :as cfg]
    [fm.simrunner.gui (core :as gui)
                      (rendering :as rdg)])
  (:import 
    (java.io File)
    (javax.swing JOptionPane)))

(def ^{:private true} all-actions #{:open-config 
                                    :save-config 
                                    :save-config-as 
                                    :run-simulation})

(defn- run-task [{worker :worker :as app} task & args]
  (send-off worker 
            (fn [_ &]
              (try
                (apply task app args)
                nil
                (catch Exception task-failed
                  (.printStackTrace task-failed)
                  nil)))))

(defmulti on-action (fn [action & _] action))

(defn- param->value [id value]
  (case id
    (:input-file :output-file) (File. (str value))
    :calc-err                  (boolean (#{"0" "1"} value))
    (str value)))

(defn- update-values [values config]
  (reduce (fn [values [id value]]
            (assoc values id (param->value id value)))
          (select-keys values [:output-file])
          config))

(defn- apply-config [app-state config file]
  (-> app-state
      (assoc :model {:config config :file file :changed? false :valid? true})
      (assoc-in [:ui :model :actions] all-actions)
      (update-in [:ui :model :values] update-values config)))

(defn- open-config [{app-state :state :as app} widget file]
  (let [config (cfg/read-config-file file)]
    (if (cfg/complete? config)
      (do
        (swap! app-state #(apply-config % config file))
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

(defmethod on-action :default [action app & args]
  (println (format "on-action{action: %s args: %s}" action args))
  (rdg/unlock! app))

(defn dispatch [action {app-state :state :as app} args]
  (when-not (-> @app-state :ui :locked?)
    (rdg/lock! app)
    (apply on-action action app args)))

