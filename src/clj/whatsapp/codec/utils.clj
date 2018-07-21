(ns whatsapp.codec.utils
  (:use [gloss.core.protocols])
  (:require [gloss.core.formats :as gf]
            [slingshot.slingshot :refer [throw+]]))


(defn take-n [n codec buf-seq]
  (loop [bytes (gf/to-buf-seq buf-seq), vals [] n n]
    (if (or (= 0 n) (empty? bytes))
      [true vals bytes]
      (let [[success v b] (read-bytes codec bytes)]
        (when-not success
          (throw+ {:message "Cannot evenly divide bytes into sequence of frames."}))
        (recur b (conj vals v) (dec n))))))
