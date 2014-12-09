(ns
  ^{:doc 
  
  "Views for the SimRunner GUI."
  
    :author "Frank Mosebach"}
  fm.simrunner.gui.view
  (:require 
    [fm.simrunner.gui (core :as gui) 
                      (toc :as toc)])  
  (:import
    (java.io File)
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

(def ^{:private true} input-specs 
                      (map #(assoc %2 :id %1)
                           input-ids
                           [{:type       :select
                             :label-text "Input File"
                             :options    {:action :select-input-file}}
                            {:type       :select
                             :label-text "Output File"
                             :options    {:action :select-output-file}}
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

(def ^{:private true} action-ids [:open-config
                                  :save-config
                                  :save-config-as
                                  :run-simulation])

(def ^{:private true} action-specs 
                      (map #(assoc %2 :action %1)
                           action-ids
                           [{:text         "Open" 
                             :tooltip-text "Open Config File"}
                            {:text         "Save" 
                             :tooltip-text "Save current Config File"}
                            {:text         "Save As" 
                             :tooltip-text "Save Config in new File"}
                            {:text         "Run" 
                             :tooltip-text "Start Simulation Run"}]))

(defn- make-buttons []
  (->> action-specs
       (map #(vector (:action %) (apply gui/button (apply concat %))))
       (into {})))                      
                      
(defn- tool-bar []
  (let [buttons        (make-buttons)
        open-button    (:open-config buttons) 
        save-button    (:save-config buttons)
        save-as-button (:save-config-as buttons)
        run-button     (:run-simulation buttons)
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

(defn- make-input [type id options]
  (apply gui/input type :id id (apply concat options)))

(defn- add-inputs 
  ([container specs]
    (add-inputs container (map #(assoc %1 :row-index %2) specs (range)) []))
  ([container specs inputs]
    (if-let [{:keys [id type label-text options row-index]} (first specs)]
      (recur container 
             (rest specs) 
             (conj inputs (add-input container 
                                     (make-input type id options) 
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
                          (.setEditable false))
        scroll-pane (doto (JScrollPane. text-area)
                          (.setPreferredSize (Dimension. 0 150)))]
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
    (toc/with-toc (gui/widget :simrunner-view main-panel
                              :contents {:tool-bar      tool-bar
                                         :config-editor config-editor
                                         :console       console}))))

