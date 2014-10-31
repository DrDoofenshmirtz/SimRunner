(ns fm.simrunner.gui
  ^{:doc 
  
  "The SimRunner GUI."
  
    :author "Frank Mosebach"}
  (:import 
    (java.awt Insets
              Dimension
              BorderLayout
              GridBagLayout
              GridBagConstraints)
    (javax.swing SwingUtilities 
                 JFrame 
                 JPanel
                 JScrollPane
                 JLabel
                 JToolBar 
                 JButton
                 JTextField
                 JTextArea
                 JCheckBox)))

(def ^{:private true} input-specs [[:select "Input File"]
                                   [:text   "EPS"]
                                   [:text   "Log Num"]
                                   [:text   "Stütz #"]
                                   [:text   "Correction"]
                                   [:text   "Max Deviation"]
                                   [:check  "Calc Err"]
                                   [:text   "ACSR Size"]])

(defn gui-exec [task]
  (let [promise (promise)]
    (if (SwingUtilities/isEventDispatchThread)
      (do 
        (deliver promise (task))
        promise)
      (do
        (SwingUtilities/invokeLater #(deliver promise (task)))
        promise))))

(defmacro gui-do [& body]
 `(gui-exec (fn [] ~@body)))

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

(defmulti input {:private true} (fn [type & _] type))

(defmethod input :text [type]
  (let [text-field (doto (JTextField.)
                         (.setColumns 16))]
    {:widget text-field}))

(defmethod input :select [type]
  (let [text-field    (doto (JTextField.)
                            (.setColumns 16)
                            (.setEditable false))
        select-button (doto (JButton. "...")
                            (.setToolTipText "Click to select"))]
    {:widget   (doto (JPanel. (BorderLayout.))
                     (.add text-field BorderLayout/CENTER)
                     (.add select-button BorderLayout/EAST))
     :contents {:text-field    text-field
                :select-button select-button}}))

(defmethod input :check [type]
  (let [check-box (doto (JCheckBox.)
                        (.setFocusPainted false))]
    {:widget check-box}))

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
                                     (input input-type) 
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

(defn simrunner-frame [& {:keys [width height] :or {width 600 height 450}}]
  (let [frame (doto (JFrame. "SimRunner")
                    (.setSize width height)
                    (.setLocationRelativeTo nil))
        view  (simrunner-view)]
    (-> frame 
        .getContentPane 
        (.setLayout (BorderLayout.)))
    (-> frame 
        .getContentPane 
        (.add (:widget view)))
    {:widget   frame
     :contents {:simrunner-view view}}))

