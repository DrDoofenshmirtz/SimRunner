(ns fm.simrunner.gui.app
  ^{:doc 
  
  "The Simrunner GUI Application."
  
    :author "Frank Mosebach"}
  (:require
    [fm.simrunner.gui (core :as gui) 
                      (view :as view) 
                      (wiring :as wiring)]))

(defmulti on-action {:private true} (fn [action & _] action))

(defmethod on-action :open-config [_ app & args]
  (println (format "open-config{args: %s}" args))
  (gui/choose-file (-> args first :widget) :title "Open Config"))  

(defmethod on-action :default [action app & args]
  (println (format "on-action{action: %s args: %s}" action args)))

(defmulti on-event {:private true} (fn [_ event-id & _] event-id))

(defmethod on-event :action-performed [app _ & args]
  (on-action (-> args first meta :action) app args))

(defmethod on-event :default [app event-id & args]
  (println (format "on-event{id: %s args: %s}" event-id args)))

(defn- app [config view]
  {:config config
   :worker (agent nil)
   :state  (atom {:ui {:view      view
                       :updating? false}})})

(defn- event-handler [app]
  (fn [& args]
    (when-not (-> app :state deref :ui :updating?)
      (apply on-event app args))))

(defn start [config]
  @(gui/gui-do
    (let [frame (view/simrunner-frame)
          view  (-> frame :contents :simrunner-view)
          frame (:widget frame)
          app   (app config view)]
      (wiring/wire-up view (event-handler app))
      (.setTitle frame "SimRunner (c) 2014 DEINC")
      (.show frame))))

