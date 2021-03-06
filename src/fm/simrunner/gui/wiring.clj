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

(defn- text-changed [input on-event]
  (on-event :input-changed input (-> input :widget .getText)))

(defmethod wire-input :text-input [input on-event]
  (-> input
      :widget
      .getDocument
      (.addDocumentListener (reify 
                              DocumentListener
                              (insertUpdate [this event]
                                (text-changed input on-event))
                              (removeUpdate [this event]
                                (text-changed input on-event))                             
                              (changedUpdate [this event]
                                (text-changed input on-event))))))

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
  (io! "Do not wire-up a view in a transaction!"
    (let [wired?   (atom true)
          on-event (fn [& args]
                     (when @wired?
                       (apply on-event args)))]
      (-> view
          (wire-toolbar-buttons on-event)
          (wire-inputs on-event)
          (vary-meta assoc ::wiring {:wired? wired?})))))

(defn wiring [view]
  (-> view meta ::wiring))

(defmacro do-unwired [view & body]
 `(let [wired?# (:wired? (wiring ~view))]
    (reset! wired?# false)
    (try
      ~@body
      (finally
        (reset! wired?# true)))))

