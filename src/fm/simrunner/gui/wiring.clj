(ns
  ^{:doc 
  
  "Wire up the Widgets of the SimRunner GUI with an Event Handler."
  
    :author "Frank Mosebach"}
  fm.simrunner.gui.wiring
  (:import
    (java.awt BorderLayout)
    (java.awt.event ActionListener)
    (javax.swing.event DocumentListener)))

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
                     (filter #(-> % meta :action)))]
    (doseq [button buttons]
      (wire-button button on-event)))
  view)

(defn- wire-inputs [view on-event]
  (doseq [input (get-in view [:contents :config-editor :contents :inputs])]
    (wire-input input on-event))
  view)

(defn wire-up [view on-event]
  (-> view
      (wire-toolbar-buttons on-event)
      (wire-inputs on-event)))

