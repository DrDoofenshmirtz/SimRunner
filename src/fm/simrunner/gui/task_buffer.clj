(ns
  ^{:doc 
  
  "A buffer for render tasks."
  
    :author "Frank Mosebach"}
  fm.simrunner.gui.task-buffer)

(defn task-buffer []
  [() ()])

(defn submit [buffer task & tasks]
  [(reduce (fn [tasks task]
             (if (nil? task)
               (throw (IllegalArgumentException. "A task must not be nil!"))
               (cons task tasks)))
           (first buffer)
           (cons task tasks)) ()])

(defn stage [buffer]
  (let [[tasks] buffer]
    [() (or tasks ())]))

(defn staged [buffer]
  (let [[_ staged] buffer]
    (reverse staged)))

(defn drain [buffer]
  (let [[tasks] buffer]
    [(or tasks ()) ()]))

