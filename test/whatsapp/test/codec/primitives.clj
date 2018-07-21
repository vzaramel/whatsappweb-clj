(ns whatsapp.test.codec.primitives
  (:require [clojure.test :refer :all]
            [whatsapp.codec.primitives :as primitives]
            [gloss.io :as gio]
            [gloss.core.formats :as gf]))

(defn ubyte->bytes [bytes]
  (map unchecked-byte bytes))

(defn bytes->binary-msg [bytes]
  (gf/to-byte-buffer
    (ubyte->bytes bytes)))

(defn decode-with-frame [frame bytes]
  (let [binary-msg (bytes->binary-msg bytes)]
    (gio/decode frame binary-msg false)))

(deftest test-codec-int20-fr
  (testing "custom frame for int 20"
    (is (= 281 (decode-with-frame primitives/int20-fr [32 1 25 99])))))

(deftest test-codec-hex-fr
  (let [decode-hex (partial decode-with-frame primitives/hex-fr)]
    (testing "decode unpack hex less than 9"
      (is (= "00" (decode-hex [0])))
      (is (= "10" (decode-hex [16])))
      (is (= "A0" (decode-hex [0xA0])))
      (is (= "05" (decode-hex [5])))
      (is (= "09" (decode-hex [9])))
      (is (not= \1\0 (decode-hex [10]))))
    (testing "decode unpack hex greater than 9"
      (is (= "0A" (decode-hex [10])))
      (is (= "0F" (decode-hex [15])))
      (is (= "F0" (decode-hex [0xF0])))))
  (let [decode (partial gio/decode primitives/hex-fr)
        encode (partial gio/encode primitives/hex-fr)]
    (testing "encode pack hex"
      (is (= "00" (decode (encode "00") true)))
      (is (= "A0" (decode (encode "A0") true)))
      (is (= "09" (decode (encode "09") true)))
      (is (= "0F" (decode (encode "0F") true)))
      (is (= "F0" (decode (encode "F0") true)))
      (is (= "10" (decode (encode "10") true)))
      (is (= "0A" (decode (encode "0A") true))))))

(deftest test-codec-packet-8-fr
  (testing "Codec packet 8 with hex-fr"
    (let [hex-unpack (primitives/create-packet-8-fr primitives/hex-fr)]
      (is (= "010C0D0" (decode-with-frame hex-unpack [4 1 12 13 2])))
      (is (= "010C0D02" (decode-with-frame
                          hex-unpack
                          [(bit-or (bit-shift-left 1 7) 4) 1 12 13 2])))))
  (testing "Codec packet 8 with niblle-fr"
    (let [nibble-unpack (primitives/create-packet-8-fr primitives/nibble-fr)]
      (is (= "01080-0.000" (decode-with-frame nibble-unpack [6 1 8 10 11 15 2])))
      (is (= "01080-0.0002" (decode-with-frame
                              nibble-unpack
                              [(bit-or (bit-shift-left 1 7) 6) 1 8 10 11 15 2]))))))

