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

(defn- make-widget
  ([type widget]
    (make-widget type widget nil))
  ([type widget options]
    (assert type "Invalid widget type!")
    (assert widget "Invalid widget!")
    (with-meta (merge {:widget widget}
                      (select-keys options [:contents]))
               (merge {:type type}
                      (select-keys options [:id])))))

(defn widget [type widget & {:as options}]
  (make-widget type widget options))

(defn button [& {:keys [action text tooltip-text] :as options}]
  (let [button (doto (JButton.)
                     (.setText text)
                     (.setToolTipText tooltip-text))
        button (make-widget :button button options)]
    (if action
      (vary-meta button assoc :action action)
      button)))

(defmulti input (fn [type & _] type))

(defmethod input :text [type & {:as options}]
  (let [text-field (doto (JTextField.)
                         (.setColumns 16))]
    (make-widget :text-input text-field options)))

(defmethod input :select [type & {:as options}]
  (let [text-field    (doto (JTextField.)
                            (.setColumns 16)
                            (.setEditable false))
        select-button (doto (JButton. "...")
                            (.setToolTipText "Click to select"))
        panel         (doto (JPanel. (BorderLayout.))
                            (.add text-field BorderLayout/CENTER)
                            (.add select-button BorderLayout/EAST))
        contents      {:select-text   (widget :select-text text-field)
                       :select-button (widget :select-button select-button)}
        options       (assoc options :contents contents)]
    (make-widget :select-input panel options)))

(defmethod input :checkbox [type & {:as options}]
  (let [checkbox (doto (JCheckBox.)
                       (.setFocusPainted false))]
    (make-widget :checkbox-input checkbox options)))

(defn frame [& {:keys [width height] :or {width 600 height 450}}]
  (let [frame (doto (JFrame.)
                    (.setSize width height)
                    (.setLocationRelativeTo nil))]
    (-> frame
        .getContentPane 
        (.setLayout (BorderLayout.)))
    (make-widget :frame frame)))

