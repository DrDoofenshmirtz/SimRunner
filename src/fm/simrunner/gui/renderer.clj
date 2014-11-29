(ns
  ^{:doc 
  
  "Defines the workflow of rendering the SimRunner ui."
  
    :author "Frank Mosebach"}
  fm.simrunner.gui.renderer
  (:require 
    [fm.simrunner.gui.core :as gui]))

(defprotocol RenderCycle
  "Defines the steps to be executed during a render cycle."
  (begin [self context] 
    "Invoked with the render context at the beginning of a cycle.")
  (render [self state] 
    "Invoked to render the state extracted from the render context.")
  (end [self result]
    "Invoked at the end of a cycle with the rendering result."))

(defn render! 
  ([render-cycle]
    (render! render-cycle nil))
  ([render-cycle context]
    (io! "Do not render in a transaction!"
         (gui/gui-do
           (->> context
                (begin render-cycle)
                (render render-cycle)
                (end render-cycle))))))

