(ns
  ^{:doc 
  
  "Rendering the the SimRunner UI."
  
    :author "Frank Mosebach"}
  fm.simrunner.gui.rendering
  (:import 
    (java.io File))
  (:require 
    [fm.simrunner.gui (core :as gui)
                      (toc :as toc)]))

(defn- start-rendering! [{app-state :state} before]
  (swap! app-state before))

(defn- stop-rendering! [{app-state :state} after]
  (swap! app-state after))

(defn- render-messages [{:keys [view model]}]
  (let [console (toc/console view)
        console (-> console :contents :text-area :widget)]
    (doseq [message (:messages model)]
      (doto console
        (.append message)
        (.append "\n")))))

(defn- update-button [button actions locked?]
  (let [action (-> button meta :action)]
    (doto (:widget button)
      (.setEnabled (boolean (and (not locked?) (actions action)))))))

(defn- render-actions [{:keys [view model locked?]}]
  (doseq [button (toc/buttons view)]
    (update-button button (:actions model) locked?)))

(defmulti set-value {:private true} (fn [input value locked?] 
                                      [(type input) (type value)]))

(defmethod set-value [:select-input Object] [input value locked?]
  (let [{:keys [select-text select-button]} (:contents input)]
    (doto (:widget select-text)
      (.setEnabled (not locked?))
      (.setText (str value)))
    (doto (:widget select-button)
      (.setEnabled (not locked?)))))

(defmethod set-value [:select-input File] [input value locked?]
  (set-value input (.getAbsolutePath value) locked?))

(defmethod set-value [:select-input nil] [input value locked?]
  (set-value input "" locked?))

(defmethod set-value [:text-input Object] [input value locked?]
  (doto (:widget input)
    (.setEnabled (not locked?))
    (.setText (str value))))

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

(defn- render-values [{:keys [view model locked?]}]
  (doseq [input (toc/inputs view)]
    (update-input input (:values model) locked?)))

(defn- render-ui [ui]
  (io! "Do not update the ui in a transaction!"
    (render-messages ui)
    (render-actions ui)
    (render-values ui)))

(defn- start-rendering [app-state]
  (if (-> app-state :ui :dirty?)
    (update-in app-state [:ui] assoc :rendering? true :dirty? false)
    app-state))

(defn- before-rendering [before]
  (if before
    (comp start-rendering before)
    start-rendering))

(defn- drop-messages [messages rendered-messages]
  (into [] (drop (count rendered-messages) messages)))

(defn- stop-rendering [app-state rendered-state]
  (let [rendered-messages (-> rendered-state :ui :model :messages)]
    (-> app-state
        (assoc-in [:ui :rendering?] false)
        (update-in [:ui :model :messages] drop-messages rendered-messages)))) 

(defn- after-rendering [after rendered-state]
  (if after
    (fn [app-state]
      (after app-state rendered-state)
      (stop-rendering app-state rendered-state))
    (fn [app-state]
      (stop-rendering app-state rendered-state))))

(defn- render-app! [app & {:keys [before after]}]
  (gui/gui-do
    (let [app-state (start-rendering! app (before-rendering before))]
      (when (-> app-state :ui :rendering?)
        (try
          (render-ui (:ui app-state))
          (finally
            (stop-rendering! app (after-rendering after app-state))))))))

(defn render! [app]
 (render-app! app))

(defn lock [app-state]
  (update-in app-state [:ui] assoc :dirty? true :locked? true))

(defn lock! [app]
  (render-app! app :before lock))

(defn unlock [app-state]
  (update-in app-state [:ui] assoc :dirty? true :locked? false))

(defn unlock! [app]
  (render-app! app :before unlock))

(defn add-messages [app-state messages]
  (if (seq messages)
    (update-in app-state [:ui :model :messages] conj messages)
    app-state))

(defn log-messages! [app & messages]
  (render-app! app :before #(add-messages % messages)))

