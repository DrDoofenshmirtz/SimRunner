(ns
  ^{:doc 
  
  "Rendering the the SimRunner ui."
  
    :author "Frank Mosebach"}
  fm.simrunner.gui.rendering
  (:import 
    (java.io File))
  (:require 
    [fm.simrunner.gui (toc :as toc)
                      (renderer :as rdr)]))

(defn- render-messages [{:keys [view model] :as ui}]
  (let [console (toc/console view)
        console (-> console :contents :text-area :widget)]
    (doseq [message (:messages model)]
      (doto console
        (.append message)
        (.append "\n")))
    ui))

(defn- update-button [button actions locked?]
  (let [action (-> button meta :action)]
    (doto (:widget button)
      (.setEnabled (boolean (and (not locked?) (actions action)))))))

(defn- render-actions [{:keys [view model locked?]:as ui}]
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

(defn- render-values [{:keys [view model locked?] :as ui}]
  (doseq [input (toc/inputs view)]
    (update-input input (:values model) locked?))
  ui)

(defn- render-ui [ui]
  (-> ui render-messages render-actions render-values))

(defn- start-rendering [ui]
  (if (:dirty? ui)
    (assoc ui :rendering? true :dirty? false)
    ui))

(defn- begin [app-state tasks]
 (update-in app-state [:ui] (apply comp start-rendering tasks)))

(defn- drop-messages [messages rendered-messages]
  (into [] (drop (count rendered-messages) messages)))

(defn- stop-rendering [app-ui rendered-ui]
  (let [rendered-messages (get-in rendered-ui [:model :messages])]
    (-> app-ui
        (assoc :rendering? false)
        (update-in [:model :messages] drop-messages rendered-messages)))) 

(defn- end [app-state ui]
  (update-in app-state [:ui] stop-rendering ui))

(deftype AppRenderCycle [app tasks]
  rdr/RenderCycle
  (rdr/begin [self _]
    (:ui (swap! (:state app) begin tasks)))
  (rdr/render [self ui]
    (if (:rendering? ui)
      (render-ui ui)
      ui))
  (rdr/end [self ui]
    (swap! (:state app) end ui)
    ui))

(defn render! [app & tasks]
 (rdr/render! (AppRenderCycle. app tasks)))

(defn lock [ui]
  (assoc ui :dirty? true :locked? true))

(defn lock! [app]
  (render! app lock))

(defn unlock [ui]
  (assoc ui :dirty? true :locked? false))

(defn unlock! [app]
  (render! app unlock))

(defn add-messages [ui messages]
  (if (seq messages)
    (-> ui
        (update-in [:model :messages] #(reduce conj % messages))
        (assoc :dirty? true))
    ui))

(defn log-messages! [app & messages]
  (render! app #(add-messages % messages)))

