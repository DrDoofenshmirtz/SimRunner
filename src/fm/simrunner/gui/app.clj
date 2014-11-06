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
   :state  (atom {:ui    {:view       view
                          :model      {}
                          :dirty?     false 
                          :rendering? false}
                  :model {}})})

(defn- rendering? [app]
  (-> app :state deref :ui :rendering?))

(defn- event-handler [app]
  (fn [& args]
    (when-not (rendering? app)
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

(defn- start-rendering! [app-state]
  (when (-> @app-state :ui :dirty?)
    (swap! app-state #(assoc-in % [:ui :rendering?] true))))

(defn- stop-rendering! [app-state]
  (when (-> @app-state :ui :rendering?)
    (swap! app-state #(-> % 
                          (assoc-in [:ui :rendering?] false)
                          (assoc-in [:ui :dirty?] false)))))

(defn render [{:keys [state] :as app}]
  (gui/gui-do
    (when (start-rendering! state)
      (try
        ;; TODO: render the view model!
        (io! "Do not update the ui in a transaction!")
        (finally
          (stop-rendering! state))))))

