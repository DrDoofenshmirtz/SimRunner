(ns fm.simrunner.app
  ^{:doc 
  
  "The SimRunner Application."
  
    :author "Frank Mosebach"}
  (:require
    [fm.simrunner (config :as config)]
    [fm.simrunner.gui (core :as gui) 
                      (view :as view) 
                      (wiring :as wiring)]))

(defn- lock [app-state]
  (update-in app-state [:ui :model :locked] inc))

(defn- lock! [{app-state :state}]
  (when (-> @app-state :ui :model :locked zero?)
    (swap! app-state lock)))

(defn- unlock [app-state]
  (update-in app-state [:ui :model :locked] #(max 0 (dec %))))

(defn- unlock! [{app-state :state}]
  (swap! app-state unlock))

(defmulti on-action {:private true} (fn [action & _] action))

(defn- run-task [{worker :worker :as app} task & args]
  (send-off worker 
            (fn [_ &]
              (try
                (apply task app args)
                nil
                (finally (unlock! app))))))

(defn- open-config [app file]
  (println (config/read-config-file file)))

(defmethod on-action :open-config [_ app & args]
  (if-let [file (gui/choose-file (-> args first :widget) :title "Open Config")]
    (run-task app open-config file)
    (unlock! app)))  

(defmethod on-action :default [action app & args]
  (println (format "on-action{action: %s args: %s}" action args))
  (unlock! app))

(defmulti on-event {:private true} (fn [_ event-id & _] event-id))

(defn- dispatch-action [action app args]
  (when (lock! app)
    (apply on-action action app args)))

(defmethod on-event :action-performed [app _ & args]
  (dispatch-action (-> args first meta :action) app args))

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
        (io! "Do not update the ui in a transaction!")
        (finally
          (stop-rendering! app))))))

