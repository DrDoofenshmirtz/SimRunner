(ns fm.simrunner.config
  (:require 
    [clojure.java.io :as jio]))

(def ^{:private true} parameter-keys [:input-file 
                                      :eps 
                                      :log-num 
                                      :stuetz-count 
                                      :correction 
                                      :max-deviation 
                                      :calc-err 
                                      :acsr-size])

(defn- blank? [trimmed-line]
  (.isEmpty trimmed-line))

(defn- commented? [trimmed-line]
  (.startsWith trimmed-line "#"))

(defn- skip-ignored [trimmed-lines]
  (filter (complement #(or (blank? %) 
                           (commented? %)))
          trimmed-lines))

(defn- strip-comment [trimmed-line]
  (let [start (.lastIndexOf trimmed-line "#")]
    (if (pos? start)
      (-> trimmed-line
          (.substring 0 start)
          .trim)
      trimmed-line)))

(defn- strip-comments [trimmed-lines]
  (map strip-comment trimmed-lines))

(defn- trimmed [lines]
  (map #(-> % str .trim) lines))

(defn read-config-lines [lines]
  (let [values (-> lines
                   trimmed
                   skip-ignored
                   strip-comments)]
    (zipmap parameter-keys values)))

(defn read-config-file [file]
  (with-open [reader (jio/reader file)]
    (read-config-lines (line-seq reader))))

