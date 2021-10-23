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

import com.google.common.base.Splitter;
import com.google.common.io.BaseEncoding;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import de.gultsch.ejabberd.api.results.Last;
import im.quicksy.server.Utils;
import im.quicksy.server.configuration.Configuration;
import im.quicksy.server.ejabberd.MyEjabberdApi;
import im.quicksy.server.throttle.RateLimiter;
import im.quicksy.server.throttle.Strategy;
import im.quicksy.server.verification.RequestFailedException;
import im.quicksy.server.verification.TokenExpiredException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.xmpp.addr.Jid;
import spark.Filter;
import spark.Route;

public class PasswordController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordController.class);

    private static final RateLimiter<InetAddress> PER_IP_RATE_LIMITER =
            RateLimiter.of(Strategy.of(Duration.ofMinutes(5), 16));

    public static Route setPassword =
            (request, response) -> {
                final String authorization = request.headers(HEADER_AUTHORIZATION);
                final String password = request.body();
                if (authorization == null) {
                    return halt(401);
                }
                if (password == null) {
                    return halt(400, "Missing password");
                }
                final String username;
                final String pin;
                try {
                    List<String> parameters =
                            Splitter.on('\u0000')
                                    .omitEmptyStrings()
                                    .limit(2)
                                    .splitToList(
                                            new String(
                                                    BaseEncoding.base64().decode(authorization)));
                    if (parameters.size() != 2) {
                        throw new IllegalArgumentException(
                                "Invalid number of authorization parameters. Was "
                                        + parameters.size());
                    }
                    username = parameters.get(0);
                    pin = parameters.get(1);
                } catch (Exception e) {
                    return halt(400, "Unable to parse authorization");
                }
                if (E164_PATTERN.matcher(username).matches()
                        && PIN_PATTERN.matcher(pin).matches()) {
                    final Phonenumber.PhoneNumber phonenumber;
                    try {
                        phonenumber = PhoneNumberUtil.getInstance().parse(username, "de");
                    } catch (NumberParseException e) {
                        return halt(400, "Unable to parse phone number");
                    }

                    final Jid jid = Utils.jidOf(phonenumber);

                    if (Configuration.getInstance().isPreventRegistration()
                            && MyEjabberdApi.getInstance()
                                            .getUserResources(
                                                    jid.getEscapedLocal(), jid.getDomain())
                                            .size()
                                    > 0) {
                        return halt(409);
                    }

                    try {
                        if (VERIFICATION_PROVIDER.verify(phonenumber, pin)) {
                            if (MyEjabberdApi.getInstance()
                                    .checkAccount(jid.getEscapedLocal(), jid.getDomain())) {
                                final Last last =
                                        MyEjabberdApi.getInstance()
                                                .getLast(jid.getEscapedLocal(), jid.getDomain());
                                final Duration lastActivity =
                                        Duration.between(last.getTimestamp(), Instant.now());
                                LOGGER.info(
                                        "user "
                                                + jid.getEscapedLocal()
                                                + " was last active "
                                                + lastActivity
                                                + " ago.");
                                if (Configuration.getInstance()
                                        .getAccountInactivity()
                                        .minus(lastActivity)
                                        .isNegative()) {
                                    LOGGER.info("delete old and create new user " + jid);
                                    MyEjabberdApi.getInstance()
                                            .unregister(jid.getEscapedLocal(), jid.getDomain());
                                    MyEjabberdApi.getInstance()
                                            .register(
                                                    jid.getEscapedLocal(),
                                                    jid.getDomain(),
                                                    password);
                                    response.status(201);
                                } else {
                                    LOGGER.info("changing password for existing user " + jid);
                                    MyEjabberdApi.getInstance()
                                            .changePassword(
                                                    jid.getEscapedLocal(),
                                                    jid.getDomain(),
                                                    password);
                                }
                            } else {
                                LOGGER.info("create new user " + jid);
                                MyEjabberdApi.getInstance()
                                        .register(jid.getEscapedLocal(), jid.getDomain(), password);
                                response.status(201);
                            }
                            return "";
                        } else {
                            LOGGER.info("verification provider reported wrong pin");
                            return halt(401);
                        }
                    } catch (TokenExpiredException e) {
                        LOGGER.warn(
                                "Contacting verification provider failed with: " + e.getMessage());
                        return halt(404);
                    } catch (RequestFailedException e) {
                        LOGGER.warn(
                                "Contacting verification provider failed with: " + e.getMessage());
                        return halt(500);
                    } catch (de.gultsch.ejabberd.api.RequestFailedException e) {
                        LOGGER.warn("Contacting ejabberd failed with: " + e.getMessage());
                        return halt(500);
                    }
                } else {
                    System.out.println("pattern didnt match");
                    return halt(400);
                }
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
