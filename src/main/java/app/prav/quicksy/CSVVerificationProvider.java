package app.prav.quicksy;

import com.google.common.util.concurrent.RateLimiter;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import im.quicksy.server.verification.AbstractVerificationProvider;
import im.quicksy.server.verification.RequestFailedException;
import im.quicksy.server.verification.TokenExpiredException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVVerificationProvider extends AbstractVerificationProvider {
    private static final String COMMA_DELIMITER = ",";
    private static final Logger LOGGER = LoggerFactory.getLogger(CSVVerificationProvider.class);
    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private final RateLimiter rateLimiter = RateLimiter.create(0.2);
    private final String csvPath;

    public CSVVerificationProvider(Map<String, String> parameter) {
        super(parameter);
        this.csvPath = parameter.get("csv_path");
    }

    @Override
    public boolean verify(Phonenumber.PhoneNumber phoneNumber, String pin)
            throws RequestFailedException {
        LOGGER.info(getInPhoneNumberFormat(phoneNumber) + " attempting " + pin);
        checkRateLimiter();
        try (BufferedReader br = new BufferedReader(new FileReader(this.csvPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(COMMA_DELIMITER);
                if (pinAndPhoneMatches(phoneNumber, pin, values)) {
                    LOGGER.info("PIN was correct");
                    return true;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Exception in reading" + this.csvPath);
            LOGGER.error(String.valueOf(e));
        }
        LOGGER.info("PIN was wrong");
        return false;
    }

    @Override
    public void request(Phonenumber.PhoneNumber phoneNumber, Method method)
            throws RequestFailedException {
        this.request(phoneNumber, method, "en");
    }

    @Override
    public void request(Phonenumber.PhoneNumber phoneNumber, Method method, String language)
            throws RequestFailedException {
        LOGGER.info(
                getInPhoneNumberFormat(phoneNumber)
                        + " has requested PIN. They should know it already");
    }

    private void checkRateLimiter() throws TokenExpiredException {
        if (!rateLimiter.tryAcquire()) {
            throw new TokenExpiredException("Rate limiter struck");
        }
    }

    private boolean pinAndPhoneMatches(
            Phonenumber.PhoneNumber phoneNumber, String pin, String[] values) {
        return values[0].equals(getInPhoneNumberFormat(phoneNumber)) && values[1].equals((pin));
    }

    private String getInPhoneNumberFormat(Phonenumber.PhoneNumber phoneNumber) {
        return this.phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    }
}
