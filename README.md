# whatsappweb-clj
**Heavily WIP**

**Whatsapp Web Client written in Clojure**

Big thanks to [sigalor](https://github.com/sigalor) and contributors of [whatsapp-web-reveng](https://github.com/sigalor/whatsapp-web-reveng) 🍻

## Dependecies
* libsodium for crypto stuff

## Features
* receive text
* get userinfo (status, presence, profilepic)
* get contacts, chats
* receive message acknowledge

## Install
```
[vzaramel/whatsapp "0.1.1"]
```
## Example
```
(require '[whatsapp.conn :as ws])

(def ws-conns (atom {})

(defn handle-every-thing
  [ws-conn & args]
  (println args))

(defn init []
  (let [ws-conn (ws/connect {:on-receive handle-every-thing
                             :on-binary handle-every-thing
                             :on-error handle-every-thing
                             :on-connect handle-every-thing
                             :on-close handle-every-thing})
        client-id (:client-id ws-conn)]
    (swap! ws-conns assoc client-id ws-conn)
    client-id))
    
(init)     
```
## TODO
* refactoring
* receive image and audio
* send text
* documention
* ...

## Legal
This code is in no way affiliated with, authorized, maintained, sponsored or endorsed by WhatsApp or any of its
affiliates or subsidiaries. This is an independent and unofficial software. Use at your own risk.
