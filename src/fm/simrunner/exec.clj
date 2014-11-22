(ns
  ^{:doc 
  
  "Utilities for the Execution of Processes."
  
    :author "Frank Mosebach"}
  fm.simrunner.exec
  (:require
    [clojure.java.io :as jio])
  (:import 
    (java.io PrintWriter)))

(defn drain-lines [source drain]
  (with-open [reader (jio/reader source)]
    (doseq [line (line-seq reader)]
      (drain line))))

(defn- print-line [^PrintWriter target ^String line]
  (.println target line))

(defn write-lines [source ^PrintWriter target]
  (drain-lines source (partial print-line target)))

(defn out>-system-out [out]
  (write-lines out *out*))

(defn err>-system-err [err]
  (write-lines err *err*))

(defn exec [path & args]
  (let [process (.start (ProcessBuilder. (into-array (cons path args))))]
    {:process process :result (delay (.waitFor process))}))

(defn drain-outputs 
  [{:keys [process result]} & {:keys [out> err>] 
                               :or   {out> out>-system-out 
                                      err> err>-system-err}}]
  {:process process :result  (delay @(future (out> (.getInputStream process)))
                                    @(future (err> (.getErrorStream process)))
                                    @result)})

(defn wait-for [{result :result}] 
  @result)

