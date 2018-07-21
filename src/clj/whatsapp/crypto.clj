(ns whatsapp.crypto
  (:require [buddy.core.mac :as mac]
            [buddy.core.bytes :as bytes]
            [buddy.core.crypto :as crypto]
            [whatsapp.utils :refer [slice]]))

(defn hash-hmac
  ([ciphertext]
   (hash-hmac ciphertext (byte-array 32)))
  ([ciphertext key]
   (mac/hash
     ciphertext
     {:key key
      :alg :hmac+sha256})))

(defn validate-shared-secret [sse secret]
  (bytes/equals?
    (hash-hmac (bytes/concat
                 (slice secret 0 32)
                 (slice secret 64))
               (slice sse 32 64))
    (slice secret 32 64)))

(defn hkdf [key length]
  (loop [key-stream (byte-array 0)
         key-block (byte-array 0)
         block-index 1]
    (if (> (count key-stream) length)
      (bytes/slice key-stream 0 length)
      (let [kb (-> (bytes/concat key-block (byte-array [block-index]))
                   (hash-hmac key))]
        (recur (bytes/concat key-stream kb) kb (inc block-index))))))

(defn decrypt-cbc
  [key text]
  (let [cipher (crypto/block-cipher :aes :cbc)
        iv     (slice text 0 16)
        input  (slice text 16)]
    (crypto/decrypt-cbc cipher input key iv)))

(defn validate-message [msg mac-key]
  (bytes/equals?
    (hash-hmac (slice msg 32) mac-key)
    (slice msg 0 32)))
