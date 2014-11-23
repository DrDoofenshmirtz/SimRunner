(defproject fm/simrunner "1.0.0"
  :description "GUI Frontend for running Simulation Tasks."
  :dependencies [[org.clojure/clojure "1.6.0"]]
  ; Don't delete libraries when executing "lein deps".                
  :disable-deps-clean true  
  :jar-name "simrunner.jar"
  ; Include all Clojure source files when executing "lein jar". 
  :omit-source false
  ; Omit all resources for now.
  :resource-paths []
  :aot [fm.simrunner.main-class 
        fm.simrunner.gui.text-field]
  :jar-exclusions [#"(?:^|/).svn/" 
                   #"(?:^|/).git/"
                   #"(?:^|/)project.clj"
                   #"(?:^|/)dist/"
                   #"(?:^|/)resources/"])

