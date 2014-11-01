(ns fm.simrunner.gui.wiring
  ^{:doc 
  
  "Wire up the Widgets of the SimRunner GUI with an Event Handler."
  
    :author "Frank Mosebach"}
  (:import 
    (java.awt BorderLayout)
    (java.awt.event ActionListener)
    (javax.swing.event DocumentListener)))

(def ^{:private true} actions #{:open-config
                                :save-config
                                :save-config-as
                                :run-simulation})

(defmulti wire-input ^{:private true} (fn [input & _] (type input)))

(defmethod wire-input :text-input [input on-event]
  (-> input
      :widget
      .getDocument
      (.addDocumentListener (reify 
                              DocumentListener
                              (insertUpdate [this event]
                                (on-event :input-changed input))
                              (removeUpdate [this event]
                                (on-event :input-changed input))
                              (changedUpdate [this event]
                                (on-event :input-changed input))))))

(defmethod wire-input :checkbox-input [input on-event]
  (-> input
      :widget
      (.addActionListener (reify
                            ActionListener
                            (actionPerformed [this event]
                              (on-event :input-changed input))))))

(defmethod wire-input :select-input [input on-event]
  (let [select-text   (-> input :contents :select-text)
        select-button (-> input :contents :select-button)]
  (-> select-button
      :widget
      (.addActionListener (reify
                            ActionListener
                            (actionPerformed [this event]
                              (on-event :input-changed input)))))))

(defn- wire-button [button on-event]
  (-> button
      :widget
      (.addActionListener (reify 
                            ActionListener
                            (actionPerformed [this event]
                              (on-event :action-performed button))))))

(defn- wire-toolbar-buttons [view on-event]
  (let [buttons (->> (get-in view [:contents :tool-bar :contents])
                     vals
                     (filter #(-> % meta :action actions)))]
    (doseq [button buttons]
      (wire-button button on-event))))

(defn- wire-inputs [view on-event]
    (doseq [input (get-in view [:contents :config-editor :contents :inputs])]
      (wire-input input on-event)))

(defn wire-up [view on-event]
  (wire-toolbar-buttons view on-event)
  (wire-inputs view on-event))

