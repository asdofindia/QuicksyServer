package de.gultsch.xmpp.addr.adapter;

import com.google.gson.GsonBuilder;
import de.gultsch.xmpp.addr.adapter.gson.JidDeserializer;
import de.gultsch.xmpp.addr.adapter.gson.JidSerializer;
import de.gultsch.xmpp.addr.adapter.sql2o.JidConverter;
import org.sql2o.converters.Converter;
import rocks.xmpp.addr.Jid;

import java.util.Map;

public class Adapter {

    public static void register(GsonBuilder gsonBuilder) {
        gsonBuilder.registerTypeAdapter(Jid.class, new JidDeserializer());
        gsonBuilder.registerTypeAdapter(Jid.class, new JidSerializer());
    }


    public static void register(Map<Class, Converter> converters) {
        final JidConverter jidConverter = new JidConverter();
        converters.put(Jid.class, jidConverter);
        try {
            converters.put(Class.forName("rocks.xmpp.addr.FullJid"), jidConverter);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

}
