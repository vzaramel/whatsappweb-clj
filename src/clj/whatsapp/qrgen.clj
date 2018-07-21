(ns whatsapp.qrgen
  (:import [net.glxn.qrgen.javase QRCode])
  (:use [clojure.java.io :only [Coercions IOFactory default-streams-impl]]))

(defmacro ^:private
  invoke-when [^QRCode qc method v]
    `(when ~v
       (~method ~qc ~v)))

(defn- with-size [^QRCode qc size]
  (.withSize qc (first size) (second size)))

(defn- with-hint [^QRCode qc hint]
  (.withHint qc (ffirst hint) (second (first hint))))

(defn- make-qrcode [str {:keys [image-type size charset correction hint]}]
  (doto
    (QRCode/from ^String str)
    (invoke-when .to image-type)
    (invoke-when with-size size)
    (invoke-when .withCharset charset)
    (invoke-when .withErrorCorrection correction)
    (invoke-when with-hint hint)))

(defn generate
  "Generate qr code image from str"
  ([str]
   (generate str {}))
  ([str opts]
   (-> (make-qrcode str opts)
       (.stream)
       (.toByteArray))))
