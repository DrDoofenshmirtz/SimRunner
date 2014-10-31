(ns fm.simrunner.config)

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

(defn read-settings [lines]
  (-> lines
      trimmed
      skip-ignored
      strip-comments))

