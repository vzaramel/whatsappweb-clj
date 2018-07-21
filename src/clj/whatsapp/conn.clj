(ns whatsapp.conn
  (:require [whatsapp.utils :refer [rand-str slice index-of-byte]]
            [whatsapp.crypto :as crypto]
            [whatsapp.codec :as codec]
            [buddy.core.codecs.base64 :as b64]
            [buddy.core.codecs :as c]
            [caesium.crypto.box :as b]
            [caesium.crypto.scalarmult :as s]
            [cheshire.core :refer [generate-string parse-string]]
            [buddy.core.bytes :as bytes]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [clojure.core.async :refer [thread chan <!! >!! dropping-buffer]]
            [caesium.byte-bufs :as bb]
            [gniazdo.core :as ws]
            [slingshot.slingshot :refer [throw+]])
  (:import (org.eclipse.jetty.util.ssl SslContextFactory)
           (org.eclipse.jetty.websocket.client WebSocketClient)))

(defn gen-client-id []
  (-> (rand-str 16)
      (b64/encode)
      (c/bytes->str)))

(defn gen-init-message [client-id]
  ["admin" "init" [0 2 9547]
   ["whatsapp-web-clj", "revenge"]
   client-id true])

(defn gen-login-message [{:whats-account/keys [client-id server-token client-token]}]
  ["admin", "login", client-token, server-token, client-id, "takeover"])

(def url "wss://w1.web.whatsapp.com/ws")

(defn gen-on-open [ws-conn]
  (fn [ws]
    (>!! (:channel @ws-conn) {:type :open})
    (println "Connected to WebSocket.")))

(defn gen-on-close [ws-conn]
  (fn [code reason]
    (>!! (:channel @ws-conn) {:type   :close
                              :code   code
                              :reason reason})
    (println "Connection to WebSocket closed.\n"
             (format "[%s] %s" code reason))))

(defn gen-on-error [ws-conn]
  (fn [e]
    (>!! (:channel @ws-conn) {:type  :error
                              :error e})))

(defn- handle-conn [ws-conn body]
  (prn "Handling Conn message")
  (when-let [secret (:secret body)]
    (let [dec-secret (b64/decode secret)
          private-key (get-in @ws-conn [:keypair :secret])
          shared-key (s/scalarmult private-key
                                   (slice dec-secret 0 32))
          sse (crypto/hkdf (crypto/hash-hmac shared-key) 80)]
      (if (crypto/validate-shared-secret sse dec-secret)
        (let [encrypted-keys (bytes/concat (slice sse 64)
                                           (slice dec-secret 64))
              decrypted-keys (crypto/decrypt-cbc (slice sse 0 32) encrypted-keys)
              enc-key (slice decrypted-keys 0 32)
              mac-key (slice decrypted-keys 32 64)
              chan (:channel @ws-conn)]
          (swap! ws-conn assoc {:enc-key enc-key
                                :mac-key mac-key})
          (>!! chan {:type :connected
                     :body body}))
        (throw+ {:message "Error generating SSE"})))))

(defn- handle-body [ws-conn body]
  (let [chan (:channel @ws-conn)]
    (case (first body)
      "Conn" (handle-conn ws-conn (second body))
      [:status 200]
      (when-let [keypair (:keypair @ws-conn)]
        (let [public (-> keypair :public bb/->bytes b64/encode c/bytes->str)
              client-id (:client-id @ws-conn)
              qr-code-content (clojure.string/join ","
                                                   [(:ref body) public client-id])]

          (>!! chan {:type            :qr-code
                     :qr-code-content qr-code-content
                     :body body})))
      (>!! chan {:type :message
                 :body body}))))

(defn- gen-handle-message [ws-conn]
  (fn [msg]
    (let [[tag body-str] (clojure.string/split msg #"," 2)
          body (parse-string body-str true)]
      (prn "Got message: " tag)
      (prn body)
      (handle-body ws-conn body))))


(defn- gen-handle-binary-message [ws-conn]
  (fn [bytes index offset]
    (prn "Got message binary:" index offset)
    (let [init-msg-index (inc (index-of-byte bytes 44))
          _ (slice bytes 0 init-msg-index)
          msg (slice bytes init-msg-index)]
      (if (crypto/validate-message msg (:mac-key @ws-conn))
        (let [dec-msg (crypto/decrypt-cbc (:enc-key @ws-conn) (slice msg 32))
              nodes (codec/decode dec-msg true)]
          (>!! chan {:type  :binary-message
                     :nodes nodes}))
        (throw+ "Invalid binary message")))))

(defn- send-msg [socket, message]
  (ws/send-msg socket (str (tc/to-long (time/now)) "," (generate-string message))))

(defn connect []
  (let [client-id (gen-client-id)
        keypair (b/keypair!)
        client (new WebSocketClient (new SslContextFactory))]
    (.setMaxTextMessageSize (.getPolicy client) (* 1024 1024 4))
    (.setMaxBinaryMessageSize (.getPolicy client) (* 1024 1024 4))
    (.start client)
    (let [ws-conn (atom {:client-id client-id
                         :channel   (chan (dropping-buffer 10))
                         :keypair   keypair})
          socket (ws/connect
                   url
                   :client client
                   :on-receive (gen-handle-message ws-conn)
                   :on-binary (gen-handle-binary-message ws-conn)
                   :on-error (gen-on-error ws-conn)
                   :on-connect (gen-on-open ws-conn)
                   :on-close (gen-on-close ws-conn)
                   :headers {"Origin" "https://web.whatsapp.com"})]
      (send-msg socket (gen-init-message client-id))
      (swap! ws-conn assoc :socket socket)
      ws-conn)))

(comment
  (def ws-conn (connect))

  (def running (atom true))

  (thread
    (let [chan (:channel @ws-conn)]
      (while @running
        (let [evt (<!! chan)]
          (prn evt))))))

