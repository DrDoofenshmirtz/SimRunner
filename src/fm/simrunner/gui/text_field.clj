(ns
  ^{:doc 
  
  "A patched JTextField implementation."
  
    :author "Frank Mosebach"}
  fm.simrunner.gui.text-field
  (:gen-class
    :name fm.simrunner.gui.TextField
    :extends javax.swing.JTextField
    :exposes-methods {setText superSetText}))

(defn -setText [this text]
  (when (not= (.getText this) text)
    (.superSetText this text)))

