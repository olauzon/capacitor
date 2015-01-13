(ns capacitor.accumulator
  (:require [clojure.core.async :refer [>!! go thread timeout alt!!]]))

;;
;; ### Accumulator
;;

(defn run!
  [e-in b-out size msecs]
  "Buffer events accumulating from `e-in` for a maximum batch of `size` or
  `msecs` milliseconds. Batches are put in `b-out`."
  (thread
    (loop [batch []
           to    (timeout msecs)]
      (alt!!
        e-in ([e]
          (if (nil? e)
            (when-not (empty? batch) (>!! b-out batch))
            (if (> size (count batch))
              (recur (conj batch e) to)
              (do
                ;;(println "size reached")
                (>!! b-out (conj batch e))
                (recur [] (timeout msecs))))))
        to ([_]
          (do
            ;;(println "timeout reached")
            (if (empty? batch)
              (recur batch (timeout msecs))
              (do
                (>!! b-out batch)
                (recur [] (timeout msecs))))))))
    (println "acc! loop stopped")))
