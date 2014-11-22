(ns
  ^{:doc 
  
  "Dispatching Input Events of a SimRunner Application."
  
    :author "Frank Mosebach"}
  fm.simrunner.input
  (:require 
    [fm.simrunner.config :as cfg]))

(defn- value->param [id value]
  (if (= :calc-err id)
    (if value "1" "0")
    (str value)))

(defn- update-values [app-state id value]
  (assoc-in app-state [:ui :model :values id] value))

(defn- update-config [app-state id value]
  (let [config (-> (get-in app-state [:model :config])
                   (cfg/with-value id (value->param id value)))]
    (-> app-state
        (assoc-in [:model :config] config)
        (assoc-in [:model :changed?] true)
        (assoc-in [:model :valid?] (cfg/complete? config)))))

(defn- on-input [id {app-state :state} & [widget value :as args]]
  (println (format "on-input{id: %s value: %s}" id value))
  (swap! app-state 
         (fn [app-state]
           (-> app-state
               (update-values id value)
               (update-config id value)))))

(defn- handle-input? [{app-state :state}]
  (let [{ui :ui} @app-state]
    (and (not (:locked? ui))
         (not (:rendering? ui)))))

(defn dispatch [id app args]
  (when (handle-input? app)
    (apply on-input id app args)))

