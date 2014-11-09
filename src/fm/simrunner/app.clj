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
  (assoc-in app-state [:ui :model :locked?] true))

(defn- lock! [{app-state :state}]
  (when-not (-> @app-state :ui :locked?)
    (view/lock (-> (swap! app-state lock) :ui :view))))

(defn- unlock [app-state]
  (assoc-in app-state [:ui :model :locked?] false))

(defn- unlock! [{app-state :state}]
  (view/unlock (-> (swap! app-state unlock) :ui :view)))

(defn- run-task [{worker :worker :as app} task & args]
  (send-off worker 
            (fn [_ &]
              (try
                (apply task app args)
                nil
                (catch Exception task-failed
                  (.printStackTrace task-failed)
                  nil)))))

(defn- mark-dirty [app-state]
  (assoc-in app-state [:ui :model :dirty?] true))

(defn- start-rendering [app-state]
  (if (-> app-state :ui :model :dirty?)
    (update-in app-state [:ui :model] assoc :rendering? true :dirty? false)
    app-state))

(defn- start-rendering! [{app-state :state}]
  (swap! app-state start-rendering))

(defn- stop-rendering [app-state unlock?]
  (let [app-state (assoc-in app-state [:ui :model :rendering?] false)]
    (if unlock?
      (assoc-in app-state [:ui :model :locked?] false) 
      app-state)))

(defn- stop-rendering! [{app-state :state} unlock?]
  (swap! app-state stop-rendering unlock?)
  (when unlock?
    (view/unlock (-> @app-state :ui :view))))

(defn- update-and-render! 
  ([app update]
    (update-and-render! app update true))
  ([{app-state :state :as app} update unlock?]
    (swap! app-state update)
    (gui/gui-do
      (let [app-state (start-rendering! app)]
        (try
          (let [{:keys [view model]} (:ui app-state)]
            (io! "Do not update the ui in a transaction!"
              (view/render view model)))
          (finally
            (stop-rendering! app unlock?)))))))

(defn- apply-config [app-state config]
  (-> app-state
      (update-in [:ui :model :input-values] merge config)
      (assoc-in [:ui :model :invalid-ids] (config/invalid-ids config))
      (assoc-in [:model :config] config)
      mark-dirty))

(defn- open-config [{app-state :state :as app} widget file]
  (let [config (config/read-config-file file)]
    (if (config/valid? config)
      (update-and-render! app #(apply-config % config))
      (gui/gui-do
        (JOptionPane/showMessageDialog (:widget widget) 
                                       "The selected config file is not valid." 
                                       "Open Config" 
                                       JOptionPane/ERROR_MESSAGE)
        (unlock! app)))))

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
  (let [blank-config (config/validate nil)
        invalid-ids  (config/invalid-ids blank-config)]
    {:config config
     :worker (agent nil)
     :state  (atom {:ui    {:view  view
                            :model {:enabled-actions #{:open-config} 
                                    :input-values    {}
                                    :invalid-values  invalid-ids
                                    :dirty?          false
                                    :locked?         false
                                    :rendering?      false}}
                    :model {:config blank-config}})}))

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

