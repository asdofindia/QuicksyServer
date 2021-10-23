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

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import com.google.common.base.Splitter;
import com.google.common.net.InetAddresses;
import im.quicksy.server.configuration.Configuration;
import im.quicksy.server.verification.MetaVerificationProvider;
import im.quicksy.server.verification.VerificationProvider;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.Request;
import spark.Route;

public class BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseController.class);

    public static Route index =
            (request, response) -> {
                response.type("text/plain");
                return "This is Quicksy Server";
            };
    protected static final String HEADER_X_REAL_IP = "X-Real-IP";
    protected static final String HEADER_AUTHORIZATION = "Authorization";

    public static Pattern E164_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    protected static Pattern PIN_PATTERN = Pattern.compile("^[0-9]{6}$");
    protected static Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    protected static final VerificationProvider VERIFICATION_PROVIDER =
            new MetaVerificationProvider();

    protected static InetAddress getClientIp(Request request) {
        final InetAddress remote = InetAddresses.forString(request.ip());
        if (remote.isLoopbackAddress()) {
            String realIp = request.headers(HEADER_X_REAL_IP);
            if (realIp != null) {
                return InetAddresses.forString(realIp);
            }
        }
        return remote;
    }

    public static Filter versionCheck =
            (request, response) -> {
                final String userAgent = request.headers("User-Agent");
                LOGGER.info("Running version check against " + userAgent);
                final List<String> parts =
                        userAgent == null
                                ? Collections.emptyList()
                                : Splitter.on('/').limit(2).splitToList(userAgent);
                if (parts.size() == 2) {
                    try {
                        Version version = Version.valueOf(parts.get(1));
                        if (!"Quicksy".equals(parts.get(0))
                                || version.lessThan(Configuration.getInstance().getMinVersion())) {
                            LOGGER.warn("Outdated client version detected (" + userAgent + ")");
                            halt(403);
                        }
                    } catch (ParseException e) {
                        LOGGER.warn(
                                "Unable to parse client version from User-Agent ("
                                        + userAgent
                                        + ")");
                        halt(403);
                    }
                } else {
                    LOGGER.warn(
                            "Unable to parse client version from User-Agent (" + userAgent + ")");
                    halt(403);
                }
            };
}
