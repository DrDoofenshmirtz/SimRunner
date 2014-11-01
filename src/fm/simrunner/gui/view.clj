(ns fm.simrunner.gui.view
  ^{:doc 
  
  "Views for the SimRunner GUI."
  
    :author "Frank Mosebach"}
  (:require 
    [fm.simrunner.gui.core :as gui])  
  (:import 
    (java.awt Insets
              Dimension
              BorderLayout
              GridBagLayout
              GridBagConstraints)
    (javax.swing JPanel
                 JScrollPane
                 JLabel
                 JToolBar 
                 JButton
                 JTextArea)))

(def ^{:private true} input-specs [[:select "Input File"]
                                   [:text   "EPS"]
                                   [:text   "Log Num"]
                                   [:text   "Stütz #"]
                                   [:text   "Correction"]
                                   [:text   "Max Deviation"]
                                   [:check  "Calc Err"]
                                   [:text   "ACSR Size"]])

(defn- tool-bar []
  (let [open-button    (doto (JButton. "Open")
                             (.setToolTipText "Open Config File")) 
        save-button    (doto (JButton. "Save")
                             (.setToolTipText "Save current Config File"))
        save-as-button (doto (JButton. "Save As")
                             (.setToolTipText "Save Config in new File"))
        run-button     (doto (JButton. "Run")
                             (.setToolTipText "Start Simulation Run"))
        tool-bar       (doto (JToolBar.)
                             (.setFloatable false)
                             (.add open-button)
                             (.add save-button)
                             (.add save-as-button)
                             (.add run-button))]
    {:widget   tool-bar
     :contents {:open-button    {:widget open-button}
                :save-button    {:widget save-button}
                :save-as-button {:widget save-as-button}
                :run-button     {:widget run-button}}}))

(defn- label-constraints [row-index]
  (GridBagConstraints. 0 row-index 1 1 0 0 
                       GridBagConstraints/WEST 
                       GridBagConstraints/NONE
                       (Insets. 2 2 2 0) 0 0))

(defn- input-constraints [row-index]
  (GridBagConstraints. 1 row-index 1 1 1 0 
                       GridBagConstraints/WEST 
                       GridBagConstraints/HORIZONTAL
                       (Insets. 2 4 2 2) 0 0))

(defn- add-input [container input label-text row-index]
  (let [label (JLabel. label-text)]
    (doto container
          (.add label 
                (label-constraints row-index))
          (.add (:widget input) 
                (input-constraints row-index)))
    input))

(defn- add-inputs 
  ([container specs]
    (add-inputs container (map conj specs (range)) []))
  ([container specs inputs]
    (if-let [[input-type label-text row-index] (first specs)]
      (recur container 
             (rest specs) 
             (conj inputs (add-input container 
                                     (gui/input input-type) 
                                     label-text 
                                     row-index)))
      inputs)))

(defn- config-editor [specs]
  (let [input-panel     (JPanel. (GridBagLayout.))
        alignment-panel (doto (JPanel. (BorderLayout.))
                              (.add input-panel BorderLayout/NORTH))
        scroll-pane     (JScrollPane. alignment-panel)]
    {:widget   scroll-pane
     :contents {:inputs (add-inputs input-panel specs)}}))

(defn- console []
  (let [text-area   (doto (JTextArea.)
                          (.setEditable false)
                          (.setPreferredSize (Dimension. 0 150)))
        scroll-pane (JScrollPane. text-area)]
    {:widget   scroll-pane
     :contents {:text-area text-area}}))

(defn simrunner-view []
  (let [tool-bar      (tool-bar)
        config-editor (config-editor input-specs)
        console       (console)
        main-panel    (doto (JPanel. (BorderLayout.))
                            (.add (:widget tool-bar) BorderLayout/NORTH)
                            (.add (:widget config-editor) BorderLayout/CENTER)
                            (.add (:widget console) BorderLayout/SOUTH))]
    {:widget   main-panel
     :contents {:tool-bar      tool-bar
                :config-editor config-editor
                :console       console}}))

(defn simrunner-frame [& {:as options}]
  (let [frame (apply gui/frame options)
        view  (simrunner-view)]
    (-> frame
        :widget
        .getContentPane 
        (.add (:widget view)))
    (assoc frame :contents {:simrunner-view view})))

