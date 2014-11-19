(ns
  ^{:doc 
  
  "Dispatching Input Events of a SimRunner Application."
  
    :author "Frank Mosebach"}
  fm.simrunner.input
  (:require 
    [fm.simrunner.config :as cfg]))

(defmulti on-input {:private true} (fn [id & _] id))

(defmethod on-input :default [id app & args]
  (println (format "on-input{id: %s args: %s}" id args)))

(defn- handle-input? [{app-state :state}]
  (let [{ui :ui} @app-state]
    (and (not (:locked? ui))
         (not (:rendering? ui)))))

(defn dispatch [id app args]
  (when (handle-input? app)
    (apply on-input id app args)))

