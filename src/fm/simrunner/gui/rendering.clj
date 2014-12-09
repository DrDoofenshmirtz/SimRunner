(ns
  ^{:doc 
  
  "Rendering the the SimRunner ui."
  
    :author "Frank Mosebach"}
  fm.simrunner.gui.rendering
  (:import 
    (java.io File))
  (:require 
    [fm.simrunner.gui (toc :as toc)
                      (wiring :as wrg)
                      (renderer :as rdr)
                      (task-buffer :as tbf)]))

(defn- start-rendering [{render-tasks :render-tasks :as ui} tasks]
  (let [render-tasks (apply tbf/submit render-tasks tasks)
        render-tasks (tbf/stage render-tasks)
        staged       (tbf/staged render-tasks)]
    (if (seq staged)
      (assoc ui :render-tasks render-tasks :rendering? true)
      ui)))

(defn- begin [app-state tasks]
 (update-in app-state [:ui] start-rendering tasks))

(defn- render [{render-tasks :render-tasks :as ui}]
  (wrg/do-unwired (:view ui)
    ((apply comp (reverse (tbf/staged render-tasks))) ui)))

(defn- stop-rendering [app-ui rendered-ui]
  (update-in app-ui [:render-tasks] tbf/drain)) 

(defn- end [app-state ui]
  (update-in app-state [:ui] stop-rendering ui))

(deftype AppRenderCycle [app tasks]
  rdr/RenderCycle
  (rdr/begin [self _]
    (:ui (swap! (:state app) begin tasks)))
  (rdr/render [self ui]
    (if (:rendering? ui)
      (render ui)
      ui))
  (rdr/end [self ui]
    (swap! (:state app) end ui)
    ui))

(defn render! [app & tasks]
 (rdr/render! (AppRenderCycle. app tasks)))

(defn lock [ui]
  (assoc ui :locked? true))

(defn unlock [ui]
  (assoc ui :locked? false))

(defn- render-messages [{view :view :as ui} messages]
  (let [console (toc/console view)
        console (-> console :contents :text-area :widget)]
    (doseq [message messages]
      (doto console
        (.append message)
        (.append "\n")))
    ui))

(defn console-logger [message & messages]
  #(render-messages % (cons message messages)))

(defn- update-button [button actions locked?]
  (let [action (-> button meta :action)]
    (doto (:widget button)
      (.setEnabled (boolean (and (not locked?) (actions action)))))))

(defn render-actions [{:keys [view model locked?]:as ui}]
  (doseq [button (toc/buttons view)]
    (update-button button (:actions model) locked?))
  ui)

(defmulti set-value {:private true} (fn [input value locked?] 
                                      [(type input) (type value)]))

(defmethod set-value [:select-input Object] [input value locked?]
  (let [{:keys [select-text select-button]} (:contents input)
        select-text   (:widget select-text)
        select-button (:widget select-button)
        value         (str value)]
    (.setEnabled select-text (not locked?))
    (.setEnabled select-button (not locked?))
    (.setText select-text value)
    (.setToolTipText select-text (when-not (.isEmpty value) value))))

(defmethod set-value [:select-input File] [input value locked?]
  (set-value input (.getAbsolutePath value) locked?))

(defmethod set-value [:select-input nil] [input value locked?]
  (set-value input "" locked?))

(defmethod set-value [:text-input Object] [input value locked?]
  (let [input (:widget input)]
    (.setEnabled input (not locked?))
    (.setText input (str value))))

(defmethod set-value [:text-input nil] [input value locked?]
  (set-value input "" locked?))

(defmethod set-value [:checkbox-input Object] [input value locked?]
  (doto (:widget input)
    (.setEnabled (not locked?))
    (.setSelected (boolean value))))

(defmethod set-value [:checkbox-input nil] [input value locked?]
  (set-value input false locked?))

(defn- update-input [input values locked?]
  (let [id    (-> input meta :id)
        value (get values id)]    
    (set-value input value locked?)))

(defn render-values [{:keys [view model locked?] :as ui}]
  (doseq [input (toc/inputs view)]
    (update-input input (:values model) locked?))
  ui)

(defn render-ui [ui]
  (-> ui render-actions render-values))

