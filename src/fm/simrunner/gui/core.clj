(ns fm.simrunner.gui.core
  ^{:doc 
  
  "GUI Utilities"
  
    :author "Frank Mosebach"}
  (:import 
    (java.awt BorderLayout)
    (javax.swing SwingUtilities 
                 JFrame 
                 JPanel
                 JButton
                 JTextField
                 JCheckBox)))

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

(defmulti input (fn [type & _] type))

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

(defn frame [& {:keys [width height] :or {width 600 height 450}}]
  (let [frame (doto (JFrame.)
                    (.setSize width height)
                    (.setLocationRelativeTo nil))]
    (-> frame
        .getContentPane 
        (.setLayout (BorderLayout.)))
    {:widget frame}))

