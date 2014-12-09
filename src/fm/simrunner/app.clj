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
                      (view :as viw) 
                      (wiring :as wrg)
                      (rendering :as rdg)
                      (task-buffer :as tbf)]
    [fm.simrunner.actions :as act])
  (:import 
    (java.io File)
    (javax.swing JFrame)))

(defmulti on-event {:private true} (fn [_ event-id & _] event-id))

(defmethod on-event :action-performed [app _ & [widget :as args]]
  (act/dispatch (-> widget meta :action) app args))

(defmethod on-event :input-changed [app _ & [widget :as args]]
  (inp/dispatch (-> widget meta :id) app args))

(defn- app [config]
  {:config config
   :worker (agent nil)
   :state  (atom {:ui    {:model        {:actions #{:open-config} 
                                         :values  {}}
                          :render-tasks (tbf/task-buffer)
                          :rendering?   false
                          :locked?      false}
                  :model {:config   (cfg/config)
                          :file     nil
                          :changed? false
                          :valid?   false}})})

(defn- event-handler [app]
  (fn [& args]
    (apply on-event app args)))

(defn- view [event-handler]
  (wrg/wire-up! (viw/simrunner-view) event-handler))

(defn- frame [view app-title stand-alone?]
  (let [frame (assoc (gui/frame) :contents {:simrunner-view view})
        frame (:widget frame)
        view  (:widget view)]    
    (.add (.getContentPane frame) view)
    (doto frame
      (.setTitle app-title)
      (.setDefaultCloseOperation (if stand-alone? 
                                   JFrame/EXIT_ON_CLOSE 
                                   JFrame/DISPOSE_ON_CLOSE)))))

(defn- with-view [app view]
  (swap! (:state app) assoc-in [:ui :view] view)
  app)

(defn start [{:keys [stand-alone? app-title working-directory] :as config}]
  @(gui/gui-do
    (let [wrkdir (-> working-directory
                     (or ".")
                     File.
                     .getAbsolutePath)
          config (assoc config :working-directory wrkdir)
          app    (app config)
          view   (view (event-handler app))
          frame  (frame view app-title stand-alone?)
          app    (with-view app view)]
      (rdg/render! app 
                   rdg/render-ui 
                   (rdg/console-logger (format "%s started." 
                                               app-title)
                                       (format "(Working directory: '%s')" 
                                               wrkdir)))
      (.show frame)
      app)))

