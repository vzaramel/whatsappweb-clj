(ns whatsapp.test.codec
  (:require [clojure.test :refer :all]
            [whatsapp.codec :as codec]
            [whatsapp.codec.primitives :as p]
            [gloss.io :as gio]
            [gloss.core.formats :as gf]
            [whatsapp.codec.primitives :as primitives]))

(defn ubyte->bytes [bytes]
  (map unchecked-byte bytes))

(defn bytes->binary-msg [bytes]
  (gf/to-byte-buffer
    (ubyte->bytes bytes)))

(defn decode-with-frame [frame bytes]
  (let [binary-msg (bytes->binary-msg bytes)]
    (gio/decode frame binary-msg false)))

(defn decode-str-binary [frame bytes]
  (let [codec (codec/create-str-binary-fr frame)]
    (decode-with-frame codec bytes)))

(deftest test-codec-hex-fr
  (let [decode (partial gio/decode (codec/create-str-binary-fr p/byte8-fr))
        encode (partial gio/encode (codec/create-str-binary-fr p/byte8-fr))]
    (testing "decode unpack hex less than 9"
      (is (= "554199991123" (decode (encode "554199991123")))))))

(deftest test-codec-str-binary
  (testing "Codec string with byte prefix"
    (is (= "abcd" (decode-str-binary :byte [4 97 98 99 100]))))
  (testing "Codec string with int20 prefix"
    (is (= "abcd" (decode-str-binary primitives/int20-fr [0 0 4 97 98 99 100]))))
  (testing "Codec string with int32 prefix"
    (is (= "abcd" (decode-str-binary :int32 [0 0 0 4 97 98 99 100]))))
  (testing "Codec string with int64 prefix"
    (is (= "abcd" (decode-str-binary :int64 [0 0 0 0 0 0 0 4 97 98 99 100])))))
