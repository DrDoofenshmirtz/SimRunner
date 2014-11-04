(ns fm.simrunner.gui.app
  ^{:doc 
  
  "The Simrunner GUI Application."
  
    :author "Frank Mosebach"}
  (:require
    [fm.simrunner.gui (core :as gui) 
                      (view :as view) 
                      (wiring :as wiring)])
  (:import 
    (java.awt BorderLayout)
    (javax.swing SwingUtilities 
                 JFrame 
                 JPanel
                 JButton
                 JTextField
                 JCheckBox)))

(defn- on-event [& args]
  (println (format "-- on-event: %s" args)))

(defn start [config]
  @(gui/gui-do
    (let [frame (view/simrunner-frame)
          view  (-> frame :contents :simrunner-view)
          frame (:widget frame)]
      (wiring/wire-up view on-event)
      (.setTitle frame "SimRunner (c) 2014 DEINC")
      (.show frame))))

