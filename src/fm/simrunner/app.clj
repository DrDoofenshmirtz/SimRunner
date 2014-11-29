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
    (java.io File)
    (javax.swing JFrame)))

(defmulti on-event {:private true} (fn [_ event-id & _] event-id))

(defmethod on-event :action-performed [app _ & [widget :as args]]
  (act/dispatch (-> widget meta :action) app args))

(defmethod on-event :input-changed [app _ & args]
  (inp/dispatch (-> args first meta :id) app args))

(defmethod on-event :default [app event-id & args]
  (println (format "on-event{id: %s args: %s}" event-id args)))

(defn- app [{:keys [app-title working-directory] :as config} view]
  (let [working-directory (-> working-directory
                              (or ".")
                              File.
                              .getAbsolutePath)
        messages          [(format "%s started." app-title)
                           (format "(Working directory: '%s')" 
                                   working-directory)]]
    {:config config
     :worker (agent nil)
     :state  (atom {:ui    {:view       view
                            :model      {:actions  #{:open-config} 
                                         :values   {}
                                         :messages messages}
                            :locked?    false
                            :dirty?     true
                            :rendering? false}
                    :model {:config   (cfg/config)
                            :file     nil
                            :changed? false
                            :valid?   false}})}))

(defn- event-handler [app]
  (fn [& args]
    (apply on-event app args)))

(defn start [{:keys [stand-alone? app-title] :as config}]
  @(gui/gui-do
    (let [frame    (view/simrunner-frame)
          view     (-> frame :contents :simrunner-view)
          frame    (:widget frame)
          on-close (if stand-alone? 
                     JFrame/EXIT_ON_CLOSE 
                     JFrame/DISPOSE_ON_CLOSE)
          app      (app config view)]
      (wrg/wire-up! view (event-handler app))
      (rdg/render! app)
      (doto frame
        (.setDefaultCloseOperation on-close)
        (.setTitle app-title)
        .show)
      app)))

