(ns whatsapp.codec.primitives
  (:use [gloss.core.protocols])
  (:require  [gloss.core.structure :refer [compile-frame]]
             [gloss.data.primitives :as gp]
             [slingshot.slingshot :refer [throw+]]
             [whatsapp.codec.utils :refer [take-n]]))

(defn- int20->int
  [x]
  (bit-and 0x000FFFFF (bit-shift-right x 8)))

(defn- unpack-byte [unpack-fn val]
  (let [val (str (unpack-fn
                   (bit-shift-right
                     (bit-and val 0xF0) 4))
                 (unpack-fn (bit-and val 0x0F)))]
    val))

(defn- pack-byte [pack-fn [first-char second-char]]
  (let [first-byte (pack-fn first-char)
        second-byte (pack-fn second-char)]
    (bit-or
      (bit-shift-left first-byte 4)
      second-byte)))

(defn- pack-hex [char]
  (if (< (byte char) (byte \A))
    (- (byte char) (byte \0))
    (+ (- (byte char) (byte \A)) 10)))


(defn- unpack-hex [val]
  (if (or (< val 0) (> val 15))
    (throw+ {:message (str "Value " val " cannot be unpacked")})
    (if (< val 10)
      (char (+ (byte \0) val))
      (char (+ (byte \A) (- val 10))))))


(defn- unpack-nibble [val]
  (cond
    (and (>= val 0) (<= val 9)) (char (+ (byte \0) val))
    (= 10 val) \-
    (= 11 val) \.
    (= 15 val) \0
    :else (throw+ {:message (str "invalid nibble to unpack: " val)})))

(defn- pack-nibble [char]
  (let [val (byte char)]
    (cond
      (and (>= val (byte \0)) (<= val (byte \9)))
      (- val (byte \0))

      (= \- char) 10
      (= \. char) 11
      (= \0 char) 15
      :else (throw+ {:message (str "invalid nibble to pack: " char)}))))


;NOTE(Zara): Fix implementation to read 3 bytes and not 4
(def int20-fr
  (gp/primitive-codec .getInt .putInt 3 int20->int int identity :be))

(def hex-fr
  (gp/primitive-codec .get .put 1
                      (partial unpack-byte unpack-hex)
                      unchecked-byte
                      (partial pack-byte pack-hex)))

(def nibble-fr
  (gp/primitive-codec .get .put 1
                      (partial unpack-byte unpack-nibble)
                      unchecked-byte
                      (partial pack-byte pack-nibble)))

(def byte8-fr
  (compose-callback
    (compile-frame :byte)
    (fn [v b]
      [true (bit-and v 0xff) b])))


(defn create-packet-8-fr [codec]
  (reify
    Reader
    (read-bytes [_ b]
      (let [[_ start-byte b] (read-bytes (compile-frame :byte) b)
            size (bit-and start-byte 0x7F)
            remove-last? (= 0 (bit-shift-right start-byte 7))
            [_ nibbles b] (take-n size codec b)
            number (apply str nibbles)]
        [true (if remove-last?
                (clojure.string/join "" (drop-last number))
                number)
         b]))
    Writer
    (sizeof [_]
      8)
    (write-bytes [_ buf v]
      nil)))
