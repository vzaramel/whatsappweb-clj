(ns whatsapp.codec
  (:use [gloss.core.protocols])
  (:require [whatsapp.utils :refer [filter-vals map-keys]]
            [whatsapp.codec.primitives :as p]
            [whatsapp.codec.utils :refer [take-n]]
            [whatsapp.codec.constants :refer [tags tokens]]
            [gloss.core :as g]
            [gloss.core.formats :as gf]
            [gloss.core.structure :refer [compile-frame]]
            [gloss.io :as gio]
            [slingshot.slingshot :refer [throw+]]
            [protobuf.impl.flatland.mapdef :as pb])
  (:import (proto Def$WebMessageInfo)))
;
;(defprotocol Reader
;              (read-bytes [this buf-seq]))
;
;(defprotocol Writer
;  (sizeof [this])
;  (write-bytes [this buf val]))


(defn get-token [index]
  (if (or (neg-int? index)
          (> index (count tokens)))
    (throw+ {:message (str "Invalid token index: " index)})
    (get tokens index)))


(defn get-token-double [index1, index2]
  (let [n (+ (* 256 index1) index2)]
    (get-token n)))

(defn get-tag-key [val]
  (ffirst
    (filter-vals tags (partial = val))))

(defn list-tag? [tag]
  (some #{:LIST_EMPTY :LIST_8 :LIST_16} [(get-tag-key tag)]))

(defn binary-tag? [tag]
  (some #{:BINARY_8 :BINARY_20 :BINARY_32} [(get-tag-key tag)]))

(defn create-dictionary-fr [dic-key]
  {dic-key (compile-frame {:type dic-key :index :byte})})

(defn create-str-binary-fr [binary-fr]
  (reify
    Reader
    (read-bytes [_ b]
      (let [[_ len b] (read-bytes (compile-frame binary-fr) b)
            [success vals b] (take-n len p/byte8-fr b)]
        (assert success)
        [true (apply str (map unchecked-char vals)) b]))
    Writer
    (sizeof [val]
      (prn val)
      8)
    (write-bytes [_ buf v]
      (prn buf)
      (prn v)
      nil)))



(declare tag-fr)

(defn jid-pair []
  (compose-callback
    (compile-frame [tag-fr tag-fr])
    (fn [[i j] b]
      [true (str i "@" j) b])))


(def tag-codecs
  (merge {:LIST_EMPTY (compile-frame g/nil-frame)
          :LIST_8     p/byte8-fr
          :LIST_16    (compile-frame :int16)
          :STREAM_END (compile-frame {:type :STREAM_END})
          :JID_PAIR   jid-pair
          :HEX_8      (p/create-packet-8-fr p/hex-fr)
          :NIBBLE_8   (p/create-packet-8-fr p/nibble-fr)
          :BINARY_8   (create-str-binary-fr p/byte8-fr)
          :BINARY_20  (create-str-binary-fr p/int20-fr)
          :BINARY_32  (create-str-binary-fr :int32)
          :BINARY_64  (create-str-binary-fr :int64)}
         (create-dictionary-fr :DICTIONARY_0)
         (create-dictionary-fr :DICTIONARY_1)
         (create-dictionary-fr :DICTIONARY_2)
         (create-dictionary-fr :DICTIONARY_3)))

(def tag-read-bytes-codec
  {:BINARY_8   (g/repeated :byte :prefix p/byte8-fr)
   :BINARY_20  (g/repeated :byte :prefix p/int20-fr)
   :BINARY_32  (g/repeated :byte :prefix :int32)})


(defn get-tag-codec [tag]
  (if (and (>= tag 3) (<= 235))
    (let [token (if (<= tag (count tokens))
                  (get-token tag)
                  nil)]
      (if (= token "s.whatsapp.net")
        "c.us"
        token))
    (let [type (get-tag-key tag)
          codec (get tag-codecs type)]
      (if (fn? codec)
        (codec)
        codec))))

(def tag-fr
  (compose-callback
    (compile-frame :byte)
    (fn [val b]
      ;(println "tag-fr ->" val)
      (let [tag-codec (get-tag-codec (int val))]
        (if (or (nil? tag-codec) (string? tag-codec))
          [true tag-codec b]
          (read-bytes tag-codec b))))))

(def list-size-fr
  (compile-frame
    (g/header
      :byte
      (fn [val]
        (if (some #{:LIST_EMPTY :LIST_8 :LIST_16} [(get-tag-key val)])
          (compile-frame (get-tag-codec val))
          (throw+ {:message (str "invalid tag for list size: " (get-tag-key val))})))
      nil)))

(defn attrs-fr [len]
  (reify
    Reader
    (read-bytes [_ b]
      (let [[success attrs b*] (take-n (* 2 len) tag-fr b)]
        (assert success)
        [true (if (even? (count attrs))
                (map-keys (apply hash-map attrs)
                          keyword)
                attrs)
         b*]))
    Writer
    (sizeof [_]
      (sizeof 8))
    (write-bytes [_ buf v]
      nil)))

(declare node-fr)

(defn list-fr [tag]
  (g/repeated node-fr
    :prefix (let [codec (get tag-codecs (get-tag-key (int tag)))]
              (if (fn? codec)
                (codec)
                codec))))


(def node-fr
  (reify
    Reader
    (read-bytes [_ b]
      (let [[_ list-size b] (read-bytes list-size-fr b)
            [_ descr-tag b] (read-bytes (compile-frame :byte) b)
            descr-codec (get-tag-codec (int descr-tag))
            [_ [descr] b] (read-bytes (compile-frame [descr-codec]) b)
            [_ attrs b] (if (number? list-size)
                          (read-bytes (attrs-fr (bit-shift-right (dec list-size) 1)) b)
                          {})]
        (if (odd? list-size)
          [true {:descr (keyword descr) :attrs attrs :content nil} b]
          (let [[_ tag b]   (read-bytes (compile-frame :byte) b)
                [_ content b] (cond
                                (list-tag? tag)
                                (read-bytes (list-fr tag) b)

                                (binary-tag? tag)
                                (read-bytes (get tag-read-bytes-codec (get-tag-key tag)) b)

                                :else
                                (read-bytes (compile-frame [(get-tag-codec (int tag))]) b))]
            [true {:descr (keyword descr) :attrs attrs :content content} b]))))
    Writer
    (sizeof [_]
      8)
    (write-bytes [_ buf v]
      nil)))

(defn decode-message [msg]
  (pb/parse  (pb/mapdef Def$WebMessageInfo)
             (byte-array msg)))

(defn decode
  ([binary-msg]
   (decode binary-msg false))
  ([binary-msg with-messages]
   (let [node (gio/decode node-fr binary-msg true)
         mapv-with-decode-msg (partial mapv
                                       #(if (= :message (:descr %))
                                          (update % :content decode-message)
                                          %))]
     (if (and with-messages (map? node) (some? (:attrs node)))
       (update node :content mapv-with-decode-msg)
       node))))
