(ns whatsapp.codec.constants)

(def tokens [nil, nil, nil, "200", "400", "404", "500", "501", "502", "action", "add",
             "after", "archive", "author", "available", "battery", "before", "body",
             "broadcast", "chat", "clear", "code", "composing", "contacts", "count",
             "create", "debug", "delete", "demote", "duplicate", "encoding", "error",
             "false", "filehash", "from", "g.us", "group", "groups_v2", "height", "id",
             "image", "in", "index", "invis", "item", "jid", "kind", "last", "leave",
             "live", "log", "media", "message", "mimetype", "missing", "modify", "name",
             "notification", "notify", "out", "owner", "participant", "paused",
             "picture", "played", "presence", "preview", "promote", "query", "raw",
             "read", "receipt", "received", "recipient", "recording", "relay",
             "remove", "response", "resume", "retry", "s.whatsapp.net", "seconds",
             "set", "size", "status", "subject", "subscribe", "t", "text", "to", "true",
             "type", "unarchive", "unavailable", "url", "user", "value", "web", "width",
             "mute", "read_only", "admin", "creator", "short", "update", "powersave",
             "checksum", "epoch", "block", "previous", "409", "replaced", "reason",
             "spam", "modify_tag", "message_info", "delivery", "emoji", "title",
             "description", "canonical-url", "matched-text", "star", "unstar",
             "media_key", "filename", "identity", "unread", "page", "page_count",
             "search", "media_message", "security", "call_log", "profile", "ciphertext",
             "invite", "gif", "vcard", "frequent", "privacy", "blacklist", "whitelist",
             "verify", "location", "document", "elapsed", "revoke_invite", "expiration",
             "unsubscribe","disable","vname","old_jid","new_jid","announcement",
             "locked","prop","label","color","call","offer","call-id"])

(def tags
  {:LIST_EMPTY   (unchecked-byte 0)
   :STREAM_END   (unchecked-byte 2)
   :DICTIONARY_0 (unchecked-byte 236)
   :DICTIONARY_1 (unchecked-byte 237)
   :DICTIONARY_2 (unchecked-byte 238)
   :DICTIONARY_3 (unchecked-byte 239)
   :LIST_8       (unchecked-byte 248)
   :LIST_16      (unchecked-byte 249)
   :JID_PAIR     (unchecked-byte 250)
   :HEX_8        (unchecked-byte 251)
   :BINARY_8     (unchecked-byte 252)
   :BINARY_20    (unchecked-byte 253)
   :BINARY_32    (unchecked-byte 254)
   :NIBBLE_8     (unchecked-byte 255)})



