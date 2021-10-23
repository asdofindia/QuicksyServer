/*
 * Copyright 2018 Daniel Gultsch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.quicksy.server.controller;

import static spark.Spark.halt;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import im.quicksy.server.Utils;
import im.quicksy.server.configuration.Configuration;
import im.quicksy.server.ejabberd.MyEjabberdApi;
import im.quicksy.server.pojo.Device;
import im.quicksy.server.throttle.RateLimiter;
import im.quicksy.server.throttle.Strategy;
import im.quicksy.server.verification.RequestFailedException;
import im.quicksy.server.verification.TwilioVerificationProvider;
import im.quicksy.server.verification.VerificationProvider;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.xmpp.addr.Jid;
import spark.Filter;
import spark.Route;

public class AuthenticationController extends BaseController {

    private static final List<String> AVAILABLE_LANGUAGES = Arrays.asList(Locale.getISOLanguages());

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);

    private static final RateLimiter<InetAddress> PER_IP_RATE_LIMITER =
            RateLimiter.of(
                    Strategy.of(Duration.ofMinutes(5), 5), Strategy.of(Duration.ofDays(1), 100));

    private static final RateLimiter<Phonenumber.PhoneNumber> PER_PHONE_NUMBER_LIMITER =
            RateLimiter.of(
                    Strategy.of(Duration.ofMinutes(2), 1), Strategy.of(Duration.ofHours(8), 2));

    private static final RateLimiter<Device> PER_DEVICE_LIMITER =
            RateLimiter.of(Strategy.of(Duration.ofDays(1), 3));

    public static Route getAuthentication =
            (request, response) -> {
                final String userSuppliedPhoneNumber = request.params("phoneNumber");
                final String installationId = request.headers("Installation-Id");
                final String userSuppliedLanguage = request.headers("Accept-Language");
                if (!E164_PATTERN.matcher(userSuppliedPhoneNumber).matches()) {
                    return halt(400, "phone number is not formatted to E164");
                }
                if (installationId == null
                        || !UUID_PATTERN.matcher(installationId).matches()
                        || userSuppliedLanguage == null) {
                    return halt(400, "Missing installation id");
                }

                final Phonenumber.PhoneNumber phoneNumber;
                try {
                    phoneNumber =
                            PhoneNumberUtil.getInstance().parse(userSuppliedPhoneNumber, "DE");
                } catch (NumberParseException e) {
                    return halt(400, "Unable to parse phone number");
                }

                if (Configuration.getInstance().isValidatePhoneNumbers()
                        && !PhoneNumberUtil.getInstance().isValidNumber(phoneNumber)) {
                    LOGGER.info("libphonenumber reported " + phoneNumber + " as invalid");
                    return halt(400);
                }

                final Jid jid = Utils.jidOf(phoneNumber);
                if (Configuration.getInstance().isPreventRegistration()
                        && MyEjabberdApi.getInstance()
                                        .getUserResources(jid.getEscapedLocal(), jid.getDomain())
                                        .size()
                                > 0) {
                    return halt(409);
                }

                try {
                    PER_PHONE_NUMBER_LIMITER.attempt(phoneNumber);
                    PER_DEVICE_LIMITER.attempt(new Device(installationId));
                } catch (RateLimiter.RetryInException e) {
                    response.header("Retry-After", String.valueOf(e.getInterval().getSeconds()));
                    LOGGER.info(e.getMessage());
                    return halt(429, e.getMessage());
                }

                final String language;
                if (AVAILABLE_LANGUAGES.contains(userSuppliedLanguage.toLowerCase())) {
                    language = userSuppliedLanguage.toLowerCase();
                } else {
                    language = null;
                }

                try {
                    VERIFICATION_PROVIDER.request(
                            phoneNumber, VerificationProvider.Method.SMS, language);
                } catch (RequestFailedException e) {
                    if (e.getCode() == TwilioVerificationProvider.PHONE_NUMBER_IS_INVALID) {
                        LOGGER.info("verification provider said " + phoneNumber + " is invalid");
                        return halt(400);
                    }
                    LOGGER.warn(
                            "unable to send SMS verification message to "
                                    + phoneNumber
                                    + " ("
                                    + e.getMessage()
                                    + ", code="
                                    + e.getCode()
                                    + ")");
                    halt(500);
                }

                return "";
            };

    public static Filter throttleIp =
            (request, response) -> {
                try {
                    PER_IP_RATE_LIMITER.attempt(getClientIp(request));
                } catch (RateLimiter.RetryInException e) {
                    LOGGER.info(e.getMessage());
                    response.header("Retry-After", String.valueOf(e.getInterval().getSeconds()));
                    halt(429, e.getMessage());
                }
            };
}
