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

(def ^{:private true} input-ids [:input-file
                                 :output-file
                                 :eps
                                 :log-num
                                 :stuetz-count
                                 :correction
                                 :max-deviation
                                 :calc-err
                                 :acsr-size])

(def ^{:private true} input-specs (map #(assoc %2 :id %1)
                                       input-ids
                                       [{:type       :select
                                         :label-text "Input File"}
                                        {:type       :select
                                         :label-text "Output File"}
                                        {:type       :text
                                         :label-text "EPS"}
                                        {:type       :text
                                         :label-text "Log Num"}
                                        {:type       :text
                                         :label-text "Stütz #"}
                                        {:type       :text
                                         :label-text "Correction"}
                                        {:type       :text
                                         :label-text "Max Deviation"}
                                        {:type       :checkbox
                                         :label-text "Calc Err"}
                                        {:type       :text
                                         :label-text "ACSR Size"}]))

(defn- tool-bar []
  (let [open-button    (gui/button :action :open-config
                                   :text "Open" 
                                   :tooltip-text "Open Config File") 
        save-button    (gui/button :action :save-config
                                   :text "Save" 
                                   :tooltip-text "Save current Config File")
        save-as-button (gui/button :action :save-config-as
                                   :text "Save As" 
                                   :tooltip-text "Save Config in new File")
        run-button     (gui/button :action :run-simulation
                                   :text "Run" 
                                   :tooltip-text "Start Simulation Run")
        tool-bar       (doto (JToolBar.)
                             (.setFloatable false)
                             (.add (:widget open-button))
                             (.add (:widget save-button))
                             (.add (:widget save-as-button))
                             (.add (:widget run-button)))]    
    (gui/widget :tool-bar tool-bar
                :contents {:open-button    open-button
                           :save-button    save-button
                           :save-as-button save-as-button
                           :run-button     run-button})))

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
    (add-inputs container (map #(assoc %1 :row-index %2) specs (range)) []))
  ([container specs inputs]
    (if-let [{:keys [id type label-text row-index]} (first specs)]
      (recur container 
             (rest specs) 
             (conj inputs (add-input container 
                                     (gui/input type :id id) 
                                     label-text 
                                     row-index)))
      inputs)))

(defn- config-editor [specs]
  (let [input-panel     (JPanel. (GridBagLayout.))
        alignment-panel (doto (JPanel. (BorderLayout.))
                              (.add input-panel BorderLayout/NORTH))
        scroll-pane     (JScrollPane. alignment-panel)]
    (gui/widget :config-editor scroll-pane
                :contents {:inputs (add-inputs input-panel specs)})))

(defn- console []
  (let [text-area   (doto (JTextArea.)
                          (.setEditable false)
                          (.setPreferredSize (Dimension. 0 150)))
        scroll-pane (JScrollPane. text-area)]
    (gui/widget :console scroll-pane
                :contents {:text-area (gui/widget :text-area text-area)})))

(defn simrunner-view []
  (let [tool-bar      (tool-bar)
        config-editor (config-editor input-specs)
        console       (console)
        main-panel    (doto (JPanel. (BorderLayout.))
                            (.add (:widget tool-bar) BorderLayout/NORTH)
                            (.add (:widget config-editor) BorderLayout/CENTER)
                            (.add (:widget console) BorderLayout/SOUTH))]
    (gui/widget :simrunner-view main-panel
                :contents {:tool-bar      tool-bar
                           :config-editor config-editor
                           :console       console})))

(defn simrunner-frame [& {:as options}]
  (let [options (apply concat options)
        frame   (apply gui/frame options)
        view    (simrunner-view)]
    (-> frame
        :widget
        .getContentPane 
        (.add (:widget view)))
    (assoc frame :contents {:simrunner-view view})))

(defn widget-seq [widget]
  (lazy-seq
    (cons widget
      (when-let [contents (vals (:contents widget))]
        (mapcat #(if (sequential? %)
                   (mapcat widget-seq %)
                   (widget-seq %))
                contents)))))

(defn- inputs [view]
  (let [input-ids (into #{} input-ids)]
    (filter (comp input-ids :id meta)
            (widget-seq view))))

(defmulti set-value {:private true} (fn [input _] (type input)))

(defmethod set-value :text-input [input value]
  (-> input
      :widget
      (.setText (str value))))

(defmethod set-value :default [input value]
  (println (format "set-value{%s value: %s}" (type input) value)))

(defn render [view model]
  (doseq [input (inputs view)]
    (let [input-id (:id (meta input))
          value    (input-id model)]
      (set-value input value))))

