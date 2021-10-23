package de.gultsch.xmpp.addr.adapter.gson;

import com.google.gson.*;
import java.lang.reflect.Type;
import rocks.xmpp.addr.Jid;

public class JidSerializer implements JsonSerializer<Jid> {

    public JsonElement serialize(
            Jid jid, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(jid.toEscapedString());
    }
}
