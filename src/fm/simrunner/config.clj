(ns fm.simrunner.config
  (:require 
    [clojure.java.io :as jio])
  (:import 
    (java.io File)))

(def ^{:private true} parameter-ids [:input-file 
                                     :eps 
                                     :log-num 
                                     :stuetz-count 
                                     :correction 
                                     :max-deviation 
                                     :calc-err 
                                     :acsr-size])

(def ^{:private true} blank-config (-> parameter-ids
                                       (zipmap (repeat nil))
                                       (assoc :invalid-ids 
                                              (into #{} parameter-ids))))
                                       
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

(defn- string->double [^String string]
  (try
    (Double/valueOf (str string))
    (catch NumberFormatException x)))

(defn- string->file [^String string]
  (let [string (-> string str .trim)]
    (when-not (.isEmpty string)
      (File. string))))

(defmulti string->value {:private true} (fn [param-id _] param-id))

(defmethod string->value :input-file [_ string] 
  (string->file string))

(defmethod string->value :eps [_ string] 
  (string->double string)) 

(defmethod string->value :log-num [_ string] 
  (string->double string)) 

(defmethod string->value :stuetz-count [_ string] 
  (string->double string)) 

(defmethod string->value :correction [_ string] 
  (string->double string)) 

(defmethod string->value :max-deviation [_ string] 
  (string->double string)) 

(defmethod string->value :calc-err [_ string] 
  (string->double string)) 

(defmethod string->value :acsr-size [_ string] 
  (string->double string)) 

(defn strings->values [config]
  (reduce (fn [config [param-id value-string]]
            (if-let [value (string->value param-id value-string)]
              (-> config
                  (assoc param-id value)
                  (update-in [:invalid-ids] disj param-id))
              (assoc config param-id nil)))
          {:invalid-ids (into #{} parameter-ids)}
          config))

(defn read-config-lines [lines]
  (let [values (-> lines
                   trimmed
                   skip-ignored
                   strip-comments)]
    (strings->values (zipmap parameter-ids values))))

(defn read-config-file [file]
  (with-open [reader (jio/reader file)]
    (read-config-lines (line-seq reader))))

(defn valid? [config]
  (-> config :invalid-ids seq not))

