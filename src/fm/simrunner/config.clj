(ns fm.simrunner.config
  (:require 
    [clojure.java.io :as jio])
  (:import 
    (java.io File)))

(defn- value->string [value]
  (let [value (-> value str .trim)]
    (when-not (.isEmpty value)
      value)))

(defn- file-value [value]
  (value->string value))

(defn- number-value [value]
  (when-let [value (value->string value)]
    (try
      (let [number (Double/valueOf value)]
        (when-not (or (Double/isNaN number) (Double/isInfinite number))
          value))
      (catch NumberFormatException x))))

(defn- boolean-value [value]
  (#{"0" "1"} (value->string value)))

(def param-ids [:input-file 
                :eps 
                :log-num 
                :stuetz-count 
                :correction 
                :max-deviation 
                :calc-err 
                :acsr-size])

(def ^{:private true} param-specs [{:value file-value}
                                   {:value number-value}
                                   {:value number-value}
                                   {:value number-value}
                                   {:value number-value}
                                   {:value number-value}
                                   {:value boolean-value}
                                   {:value number-value}])

(def ^{:private true} param-defs (zipmap param-ids param-specs))

(defn- param-value [param-id value]
  ((get-in param-defs [param-id :value]) value))

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

(defn config
  ([]
    (config {}))
  ([map]
    (reduce (fn [config param-id]
              (if-let [value (param-value param-id (param-id map))]
                (assoc config param-id value)
                config))
            {}
            param-ids)))

(defn complete? [config]
  (and config (every? config param-ids)))

(defn read-config-lines [lines]
  (let [values (-> lines
                   trimmed
                   skip-ignored
                   strip-comments)]
    (-> param-ids (zipmap values) config)))

(defn read-config-file [file]
  (with-open [reader (jio/reader file)]
    (read-config-lines (line-seq reader))))

(defn with-value [config param-id value]
  (if-let [param-def (get param-defs param-id)]
    (when-let [value (param-value param-id value)]
      (assoc config param-id value))
    (throw (IllegalArgumentException. 
             (format "Undefined config parameter: '%s'!" param-id)))))

(defn write-config-file [config file]
  (with-open [writer (jio/writer file)]
    (doseq [value (map #(str (get config %)) param-ids)]
      (doto writer (.write value) .newLine))))

