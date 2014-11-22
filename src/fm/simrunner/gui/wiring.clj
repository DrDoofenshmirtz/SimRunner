(ns
  ^{:doc 
  
  "Wire up the Widgets of the SimRunner GUI with an Event Handler."
  
    :author "Frank Mosebach"}
  fm.simrunner.gui.wiring
  (:require
    [fm.simrunner.gui.toc :as toc])
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
                                (on-event :input-changed 
                                          input 
                                          (-> input :widget .getText)))
                              (removeUpdate [this event]
                                (on-event :input-changed 
                                          input 
                                          (-> input :widget .getText)))
                              (changedUpdate [this event]
                                (on-event :input-changed 
                                          input 
                                          (-> input :widget .getText)))))))

(defmethod wire-input :checkbox-input [input on-event]
  (-> input
      :widget
      (.addActionListener (reify
                            ActionListener
                            (actionPerformed [this event]
                              (on-event :input-changed 
                                        input 
                                        (-> input :widget .isSelected)))))))

(defmethod wire-input :select-input [input on-event]
  (let [select-button (-> input :contents :select-button)]
    (-> select-button
        :widget
        (.addActionListener (reify
                              ActionListener
                              (actionPerformed [this event]
                                (on-event :action-performed input)))))))

(defn- wire-button [button on-event]
  (-> button
      :widget
      (.addActionListener (reify 
                            ActionListener
                            (actionPerformed [this event]
                              (on-event :action-performed button))))))

(defn- wire-toolbar-buttons [view on-event]
  (doseq [button (toc/buttons view)]
    (wire-button button on-event))
  view)

(defn- wire-inputs [view on-event]
  (doseq [input (toc/inputs view)]
    (wire-input input on-event))
  view)

(defn wire-up! [view on-event]
  (io!
    (-> view
        (wire-toolbar-buttons on-event)
        (wire-inputs on-event))))

