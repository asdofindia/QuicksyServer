package im.quicksy.server.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.regex.Pattern;

public class PatternDeserializer implements JsonDeserializer<Pattern> {
    @Override
    public Pattern deserialize(
            final JsonElement jsonElement,
            final Type type,
            final JsonDeserializationContext context)
            throws JsonParseException {
        if (jsonElement.isJsonNull()) {
            return null;
        }
        final String pattern = jsonElement.getAsString();
        try {
            return Pattern.compile(pattern);
        } catch (Exception e) {
            throw new JsonParseException("invalid pattern", e);
        }
    }
}
