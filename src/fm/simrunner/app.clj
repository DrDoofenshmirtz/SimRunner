(ns 
  ^{:doc 
  
  "The SimRunner Application."
  
    :author "Frank Mosebach"}
  fm.simrunner.app
  (:require
    [fm.simrunner (config :as cfg)
                  (actions :as act)
                  (input :as inp)]
    [fm.simrunner.gui (core :as gui) 
                      (view :as view) 
                      (wiring :as wrg)
                      (rendering :as rdg)]
    [fm.simrunner.actions :as act])
  (:import 
    (javax.swing JOptionPane)))

(defmulti on-event {:private true} (fn [_ event-id & _] event-id))

(defmethod on-event :action-performed [app _ & [widget :as args]]
  (act/dispatch (-> widget meta :action) app args))

(defmethod on-event :input-changed [app _ & args]
  (inp/dispatch (-> args first meta :id) app args))

(defmethod on-event :default [app event-id & args]
  (println (format "on-event{id: %s args: %s}" event-id args)))

(defn- app [config view]
    {:config config
     :worker (agent nil)
     :state  (atom {:ui    {:view       view
                            :model      {:actions  #{:open-config} 
                                         :values   {}
                                         :messages ["SimRunner is ready."]}
                            :locked?    false
                            :dirty?     true
                            :rendering? false}
                    :model {:config   (cfg/config)
                            :file     nil
                            :changed? false
                            :valid?   false}})})

(defn- event-handler [app]
  (fn [& args]
    (apply on-event app args)))

(defn start [config]
  @(gui/gui-do
    (let [frame (view/simrunner-frame)
          view  (-> frame :contents :simrunner-view)
          frame (:widget frame)
          app   (app config view)]
      (wrg/wire-up! view (event-handler app))
      (rdg/render! app)
      (.setTitle frame "SimRunner (c) 2014 DEINC")
      (.show frame))))

