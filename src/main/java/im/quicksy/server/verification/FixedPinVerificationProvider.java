package im.quicksy.server.verification;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.math.BigInteger;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixedPinVerificationProvider extends AbstractVerificationProvider {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FixedPinVerificationProvider.class);

    private final String salt;

    public FixedPinVerificationProvider(final Map<String, String> parameter) {
        super(parameter);
        this.salt = Strings.nullToEmpty(parameter.get("salt"));
    }

    @Override
    public boolean verify(final Phonenumber.PhoneNumber phoneNumber, final String pin)
            throws RequestFailedException {
        final boolean verified = generatePin(phoneNumber).equals(pin);
        if (verified) {
            LOGGER.info("Pin for {} has been verified successfully", phoneNumber);
            return true;
        } else {
            LOGGER.info("Pin for {} was incorrect", phoneNumber);
            return false;
        }
    }

    @Override
    public void request(final Phonenumber.PhoneNumber phoneNumber, final Method method)
            throws RequestFailedException {
        final String pin = generatePin(phoneNumber);
        LOGGER.info("requesting pin for {}. Pin is going to be {}", phoneNumber, pin);
    }

    @Override
    public void request(Phonenumber.PhoneNumber phoneNumber, Method method, String language)
            throws RequestFailedException {
        this.request(phoneNumber, method);
    }

    @SuppressWarnings("UnstableApiUsage")
    private String generatePin(final Phonenumber.PhoneNumber phoneNumber) {
        final String e164 =
                PhoneNumberUtil.getInstance()
                        .format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        return new BigInteger(
                        1,
                        Hashing.sha256()
                                .newHasher()
                                .putString(e164, Charsets.UTF_8)
                                .putChar('\0')
                                .putString(salt, Charsets.UTF_8)
                                .hash()
                                .asBytes())
                .toString()
                .substring(0, 6);
    }
}
