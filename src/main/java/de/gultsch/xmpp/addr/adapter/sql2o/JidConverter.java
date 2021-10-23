package de.gultsch.xmpp.addr.adapter.sql2o;

import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import rocks.xmpp.addr.Jid;

public class JidConverter implements Converter<Jid> {

    @Override
    public Jid convert(Object o) throws ConverterException {
        if (o == null) {
            return null;
        } else if (o instanceof String) {
            try {
                return Jid.of((String) o);
            } catch (IllegalArgumentException e) {
                throw new ConverterException("Unable to convert " + o.toString(), e);
            }
        } else {
            throw new ConverterException(
                    "can not convert object of type " + o.getClass().getName() + " to Jid");
        }
    }

    @Override
    public Object toDatabaseParam(Jid jid) {
        return jid.asBareJid().toString();
    }
}
