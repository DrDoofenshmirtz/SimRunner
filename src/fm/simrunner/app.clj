(ns fm.simrunner.app
  ^{:doc 
  
  "The SimRunner Application."
  
    :author "Frank Mosebach"}
  (:require
    [fm.simrunner (config :as config)]
    [fm.simrunner.gui (core :as gui) 
                      (view :as view) 
                      (wiring :as wiring)])
  (:import 
    (javax.swing JOptionPane)))

(defn- lock [app-state]
  (update-in app-state [:ui :model :locked] inc))

(defn- lock! [{app-state :state}]
  (when (-> @app-state :ui :model :locked zero?)
    (swap! app-state lock)))

(defn- unlock [app-state]
  (update-in app-state [:ui :model :locked] #(max 0 (dec %))))

(defn- unlock! [{app-state :state}]
  (swap! app-state unlock))

(defn- run-task [{worker :worker :as app} task & args]
  (send-off worker 
            (fn [_ &]
              (try
                (apply task app args)
                nil
                (finally (unlock! app))))))

(defn- start-rendering [app-state]
  (assoc-in app-state [:ui :model :rendering?] true))

(defn- start-rendering! [{app-state :state}]
  (when (-> @app-state :ui :model :dirty?)
    (swap! app-state start-rendering)))

(defn- stop-rendering [app-state]
  (-> app-state 
      (assoc-in [:ui :model :rendering?] false)
      (assoc-in [:ui :model :dirty?] false)))

(defn- stop-rendering! [{app-state :state}]
  (when (-> @app-state :ui :model :rendering?)
    (swap! app-state stop-rendering)))

(defn render [app]
  (gui/gui-do
    (when (start-rendering! app)
      (try
        (let [{:keys [view model]} (-> app :state deref :ui)]
          (io! "Do not update the ui in a transaction!"
            (view/render view (:inputs model))))
        (finally
          (stop-rendering! app))))))

(defn- open-config [{app-state :state :as app} widget file]
  (let [config (config/read-config-file file)]
    (if (config/valid? config)
      (do
        (swap! app-state #(-> % 
                              (assoc-in [:ui :model :inputs :eps] "Mausi")
                              (assoc-in [:ui :model :dirty?] true)))
        (render app))
      @(gui/gui-do
        (JOptionPane/showMessageDialog (:widget widget) 
                                       "The selected config file is not valid." 
                                       "Open Config" 
                                       JOptionPane/ERROR_MESSAGE)))))

(defmulti on-action {:private true} (fn [action & _] action))

(defmethod on-action :open-config [_ app & [widget]]
  (if-let [file (gui/choose-file (:widget widget) :title "Open Config")]
    (run-task app open-config widget file)
    (unlock! app)))  

(defmethod on-action :default [action app & args]
  (println (format "on-action{action: %s args: %s}" action args))
  (unlock! app))

(defn- dispatch-action [action app args]
  (when (lock! app)
    (apply on-action action app args)))

(defmulti on-input {:private true} (fn [id & _] id))

(defmethod on-input :default [id app & args]
  (println (format "on-input{id: %s args: %s}" id args)))

(defn- handle-input? [{app-state :state}]
  (let [model (-> @app-state :ui :model)]
    (and (not (:locked? model))
         (not (:rendering? model)))))

(defn- input-changed [id app args]
  (when (handle-input? app)
    (apply on-input id app args)))

(defmulti on-event {:private true} (fn [_ event-id & _] event-id))

(defmethod on-event :action-performed [app _ & args]
  (dispatch-action (-> args first meta :action) app args))

(defmethod on-event :input-changed [app _ & args]
  (input-changed (-> args first meta :id) app args))

(defmethod on-event :default [app event-id & args]
  (println (format "on-event{id: %s args: %s}" event-id args)))

(defn- app [config view]
  {:config config
   :worker (agent nil)
   :state  (atom {:ui    {:view  view
                          :model {:inputs     {}
                                  :dirty?     false
                                  :locked     0
                                  :rendering? false}}
                  :model {}})})

(defn- event-handler [app]
  (fn [& args]
    (apply on-event app args)))

(defn start [config]
  @(gui/gui-do
    (let [frame (view/simrunner-frame)
          view  (-> frame :contents :simrunner-view)
          frame (:widget frame)
          app   (app config view)]
      (wiring/wire-up view (event-handler app))
      (.setTitle frame "SimRunner (c) 2014 DEINC")
      (.show frame))))

