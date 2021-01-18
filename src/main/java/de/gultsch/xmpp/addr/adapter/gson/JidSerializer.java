package de.gultsch.xmpp.addr.adapter.gson;

import com.google.gson.*;
import rocks.xmpp.addr.Jid;

import java.lang.reflect.Type;

public class JidSerializer implements JsonSerializer<Jid> {

    public JsonElement serialize(Jid jid, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(jid.toEscapedString());
    }
}
