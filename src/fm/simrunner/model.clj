(ns
  ^{:doc 
  
  "Updating the SimRunner Application Model."
  
    :author "Frank Mosebach"}
  fm.simrunner.model
  (:require
    [fm.simrunner.config :as cfg])
  (:import 
    (java.io File)))

(defn- value->param [id value]
  (case id
    (:input-file :output-file) (and value (.getAbsolutePath value))
    :calc-err                  (if value "1" "0")
    (str value)))

(defn- update-values [app-state id value]
  (assoc-in app-state [:ui :model :values id] value))

(defn- update-config [app-state id value]
  (if (= :output-file id)
    app-state
    (let [config (-> (get-in app-state [:model :config])
                     (cfg/with-value id (value->param id value)))]
      (-> app-state
          (assoc-in [:model :config] config)
          (assoc-in [:model :changed?] true)
          (assoc-in [:model :valid?] (cfg/complete? config))))))

(defn- update-actions [{:keys [ui model]:as app-state}]
  (let [valid?   (:valid? model)
        changed? (:changed? model)
        actions  (case [valid? changed?]
                   [false false] #{:open-config}
                   [false  true] #{:open-config}
                   [true  false] #{:open-config :save-config-as :run-simulation}
                   #{:open-config :save-config :save-config-as})]
    (-> app-state 
        (assoc-in [:ui :model :actions] actions)
        (assoc-in [:ui :dirty?] true))))

(defn update-value [app-state id value]
  (-> app-state
      (update-values id value)
      (update-config id value)
      update-actions))

(defn- param->value [id value]
  (case id
    (:input-file :output-file) (File. (str value))
    :calc-err                  (= "1" value)
    (str value)))

(defn- apply-params [values config]
  (reduce (fn [values [id value]]
            (assoc values id (param->value id value)))
          (select-keys values [:output-file])
          config))

(defn apply-config [app-state config file]
  (-> app-state
      (assoc :model {:config   config 
                     :file     file 
                     :changed? false 
                     :valid?   (cfg/complete? config)})
      (update-in [:ui :model :values] apply-params config)
      update-actions))

