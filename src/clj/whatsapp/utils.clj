(ns whatsapp.utils
  (:require [buddy.core.bytes :as bytes]))

(defn slice
  ([byte-array init]
   (bytes/slice byte-array init (count byte-array)))
  ([byte-array init final]
   (bytes/slice byte-array init final)))

(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(defn map-keys
  [m f]
  (persistent!
    (reduce-kv (fn [m k v] (assoc! m (f k) v))
               (transient (empty m)) m)))

(defn filter-vals
  [m pred]
  (persistent!
    (reduce-kv (fn [acc k v] (if (pred v) (conj! acc [k v]) acc))
               (transient (empty m)) m)))

(defn index-of-byte [msg b]
  (loop [i 0
         [n & rest] msg]
    (if (= (byte b) n)
      i
      (recur (inc i) rest))))
