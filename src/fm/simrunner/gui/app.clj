(ns fm.simrunner.gui.app
  ^{:doc 
  
  "The Simrunner GUI Application."
  
    :author "Frank Mosebach"}
  (:require
    [fm.simrunner.gui (core :as gui) 
                      (view :as view) 
                      (wiring :as wiring)])
  (:import 
    (java.awt BorderLayout)
    (javax.swing SwingUtilities 
                 JOptionPane)))

(defmulti on-action {:private true} (fn [action & _] action))

(defmethod on-action :open-config [_ model & args]
  (println (format "open-config{model: %s args: %s}" model args))
  (gui/choose-file (-> args first :widget) :title "Open Config"))  

(defmethod on-action :default [action model & args]
  (println (format "on-action{action: %s model: %s args: %s}" 
                   action model args)))

(defmulti on-event {:private true} (fn [_ event-id & _] event-id))

(defmethod on-event :action-performed [model _ & args]
  (on-action (-> args first meta :action) model args))

(defmethod on-event :default [model event-id & args]
  (println (format "on-event{model: %s id: %s args: %s}" model event-id args)))

(defn start [config]
  @(gui/gui-do
    (let [frame (view/simrunner-frame)
          view  (-> frame :contents :simrunner-view)
          frame (:widget frame)
          model (ref {:app-config config
                      :worker     (agent nil)})]
      (wiring/wire-up view (partial on-event model))
      (.setTitle frame "SimRunner (c) 2014 DEINC")
      (.show frame))))

