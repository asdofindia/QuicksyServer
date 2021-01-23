package im.quicksy.server.verification;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.math.IntMath;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.Phonenumber;
import im.quicksy.server.configuration.Configuration;
import im.quicksy.server.verification.nexmo.GenericResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class NexmoVerificationProvider implements VerificationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NexmoVerificationProvider.class);

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            //.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build();

    private static final Gson GSON = new GsonBuilder().create();

    private static final HttpUrl NEXMO_API_URL = HttpUrl.get("https://rest.nexmo.com/sms/json");

    private static final String BRAND_NAME = "Quicksy.im";
    private static final String MESSAGE = "Your Quicksy code is: %s\n\nDon't share this code with others.\n\nOYITl6r6eIp";

    private static final List<Integer> COUNTRY_CODES_SUPPORTING_ALPHA_NUMERIC = Arrays.asList(
            49
    );

    private static final int MAX_ATTEMPTS = 3;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final Cache<Phonenumber.PhoneNumber, Pin> PIN_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    @Override
    public boolean verify(Phonenumber.PhoneNumber phoneNumber, String input) throws RequestFailedException {
        final Pin pin = PIN_CACHE.getIfPresent(phoneNumber);
        if (pin == null) {
            throw new TokenExpiredException("No pin found for this phone number");
        }
        try {
            return pin.verify(input);
        } catch (TooManyAttemptsException e) {
            throw new TokenExpiredException(e);
        }
    }

    @Override
    public void request(Phonenumber.PhoneNumber phoneNumber, Method method) throws RequestFailedException {
        final Pin pin = Pin.generate();
        PIN_CACHE.put(phoneNumber, pin);
        final String to = String.format("%d%d", phoneNumber.getCountryCode(), phoneNumber.getNationalNumber());
        final String nexmoPhoneNumber = Configuration.getInstance().getNexmoPhoneNumber();
        final String from;
        if (Strings.isNullOrEmpty(nexmoPhoneNumber) || COUNTRY_CODES_SUPPORTING_ALPHA_NUMERIC.contains(phoneNumber.getCountryCode())) {
            from = BRAND_NAME;
        } else {
            from = nexmoPhoneNumber;
        }
        LOGGER.info("requesting SMS through nexmo for {}", to);
        final Call call = OK_HTTP_CLIENT.newCall(new Request.Builder()
                .post(new FormBody.Builder()
                        .add("from", from)
                        .add("text", String.format(MESSAGE, pin.toString()))
                        .add("to", to)
                        .add("api_key", Configuration.getInstance().getNexmoApiKey())
                        .add("api_secret", Configuration.getInstance().getNexmoApiSecret())
                        .build())
                .url(NEXMO_API_URL)
                .build());
        try {
            final Response response = call.execute();
            final int code = response.code();
            if (code != 200) {
                LOGGER.warn("failed to request SMS verification. error code was {}", code);
                throw new RequestFailedException("Response code was " + code);
            } else {
                final ResponseBody body = response.body();
                if (body == null) {
                    throw new RequestFailedException("Empty body");
                }
                final GenericResponse nexmoResponse = GSON.fromJson(body.charStream(), GenericResponse.class);
                final List<GenericResponse.Message> messages = nexmoResponse.getMessages();
                if (messages.size() >= 1) {
                    final GenericResponse.Message message = messages.get(0);
                    final String status = message.getStatus();
                    if (!"0".equals(status)) {
                        LOGGER.error("Unable to requests SMS. Status={} text={}", message.getStatus(), message.getErrorText());
                        throw new RequestFailedException(message.getErrorText());
                    }
                } else {
                    throw new RequestFailedException("Invalid number of result messages");
                }
            }
            LOGGER.info("call was successful");
        } catch (IOException e) {
            LOGGER.warn("failed to request SMS verification", e);
            throw new RequestFailedException(e);
        }
    }

    @Override
    public void request(Phonenumber.PhoneNumber phoneNumber, Method method, String language) throws RequestFailedException {
        request(phoneNumber, method);
    }

    public static class Pin {
        private final String pin;
        private int attempts = 0;

        Pin(String pin) {
            this.pin = pin;
        }

        public static Pin generate() {
            final int pin = SECURE_RANDOM.nextInt(IntMath.pow(10, VerificationProvider.VERIFICATION_CODE_LENGTH));
            return new Pin(Strings.padStart(
                    String.valueOf(pin),
                    VerificationProvider.VERIFICATION_CODE_LENGTH,
                    '0'
            ));
        }

        public synchronized boolean verify(String pin) {
            if (this.attempts >= MAX_ATTEMPTS) {
                throw new TooManyAttemptsException();
            }
            this.attempts++;
            return this.pin.equals(pin);
        }

        @Override
        public String toString() {
            return this.pin;
        }
    }

    public static class TooManyAttemptsException extends RuntimeException {

    }
}
