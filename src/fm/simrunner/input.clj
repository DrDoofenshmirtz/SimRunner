(ns
  ^{:doc 
  
  "Dispatching the Input Events of a SimRunner Application."
  
    :author "Frank Mosebach"}
  fm.simrunner.input
  (:require
    [fm.simrunner.model :as mod]
    [fm.simrunner.gui.rendering :as rdg]))

(defn- on-input [id {app-state :state :as app} & [_ value]]
  (swap! app-state mod/update id value) 
  (rdg/render! app))

(defn- handle-input? [{app-state :state}]
  (let [{ui :ui} @app-state]
    (and (not (:locked? ui))
         (not (:rendering? ui)))))

(defn dispatch [id app args]
  (when (handle-input? app)
    (apply on-input id app args)))

