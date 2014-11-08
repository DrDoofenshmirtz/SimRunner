(ns fm.simrunner.config
  (:require 
    [clojure.java.io :as jio])
  (:import 
    (java.io File)))

(defn- string->file [^String string]
  (let [string (-> string str .trim)]
    (when-not (.isEmpty string)
      (File. string))))

(defn- file? [value]
  (instance? File value))

(defn- string->double [^String string]
  (try
    (Double/valueOf (str string))
    (catch NumberFormatException x)))

(defn- string->boolean [^String string]
  (let [string (-> string str .trim)]
    (cond
      (= "1" string) true
      (= "0" string) false
      :else nil)))

(defn- boolean? [value]
  (instance? Boolean value))

(def ^{:private true} parameter-ids [:input-file 
                                     :eps 
                                     :log-num 
                                     :stuetz-count 
                                     :correction 
                                     :max-deviation 
                                     :calc-err 
                                     :acsr-size])

(def ^{:private true} parameter-specs [{:valid?        file?
                                        :string->value string->file}
                                       {:valid?        number?
                                        :string->value string->double}
                                       {:valid?        number?
                                        :string->value string->double}
                                       {:valid?        number?
                                        :string->value string->double}
                                       {:valid?        number?
                                        :string->value string->double}
                                       {:valid?        number?
                                        :string->value string->double}
                                       {:valid?        boolean?
                                        :string->value string->boolean}
                                       {:valid?        number?
                                        :string->value string->double}])

(def ^{:private true} parameter-defs (zipmap parameter-ids parameter-specs))
     
(defn- throw-undefined-param-id [id]
  (throw (IllegalArgumentException. (format "Undefined parameter id: %s!" id))))

(defn string->value [param-id value-string]
  (if-let [string->value (get-in parameter-defs [param-id :string->value])]
    (string->value value-string)
    (throw-undefined-param-id param-id)))

(defn value-valid? [param-id value]
  (if-let [valid? (get-in parameter-defs [param-id :valid?])]
    (valid? value)
    (throw-undefined-param-id param-id)))

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

(defn strings->values [config]
  (->> (merge (zipmap parameter-ids (repeat nil)) config)
       (map (fn [[param-id value-string]]
              [param-id (string->value param-id value-string)]))
       (into {})))

(defn validate [config]
  (reduce (fn [validated param-id]
            (let [value (param-id config)]
              (if (value-valid? param-id value)
                (-> validated
                    (assoc param-id value)
                    (vary-meta update-in [::invalid-ids] disj param-id))
                (assoc validated param-id nil))))
          (with-meta {}
                     (assoc (meta config) 
                            :type         ::config
                            ::invalid-ids (into #{} parameter-ids)))
          parameter-ids))

(defn invalid-ids
  ([config]
    (invalid-ids config false))
  ([config validate?]
    (-> (if validate? (validate config) config) meta ::invalid-ids)))

(defn config? [config]
  (= ::config (type config)))

(defn valid? 
  ([config]
    (valid? config false))
  ([config validate?]
    (let [config (if validate? (validate config) config)]
      (and (config? config)
           (-> config invalid-ids seq not)))))

(defn read-config-lines [lines]
  (let [values (-> lines
                   trimmed
                   skip-ignored
                   strip-comments)]
    (-> parameter-ids
        (zipmap values)
        strings->values
        validate)))

(defn read-config-file [file]
  (with-open [reader (jio/reader file)]
    (read-config-lines (line-seq reader))))

