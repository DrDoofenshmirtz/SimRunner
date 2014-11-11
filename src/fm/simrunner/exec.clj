(ns
  ^{:doc 
  
  "Utilities for the Execution of Processes."
  
    :author "Frank Mosebach"}
  fm.simrunner.exec
  (:require
    [clojure.java.io :as jio])
  (:import 
    (java.io InputStream PrintWriter)))

(defn write-lines [^InputStream source ^PrintWriter target]
  (with-open [reader (jio/reader source)]
    (doseq [line (line-seq reader)]
      (.println target line))))

(defn out>-system-out [out]
  (write-lines out *out*))

(defn err>-system-err [err]
  (write-lines err *err*))

(defn exec [path & args]
  (.start (ProcessBuilder. (into-array (cons path args)))))

(defn drain-outputs 
  [process & {:keys [out> err>] 
              :or {out> out>-system-out err> err>-system-err}}]
  (future (out> (.getInputStream process)))
  (future (err> (.getErrorStream process)))
  nil)

