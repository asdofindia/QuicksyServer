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

package im.quicksy.server.utils;

import com.google.common.net.UrlEscapers;
import im.quicksy.server.configuration.Configuration;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.xmpp.addr.Jid;

public class CimUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CimUtils.class);

    public static boolean isPayingAccount(final Jid jid) {
        final Optional<String> cimAuthToken = Configuration.getInstance().getCimAuthToken();
        if (!cimAuthToken.isPresent()) {
            return false;
        }
        try {
            URL url =
                    new URL(
                            "https://account.conversations.im/api/get-paying-account/"
                                    + UrlEscapers.urlPathSegmentEscaper()
                                            .escape(jid.toEscapedString())
                                    + "/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", cimAuthToken.get());
            final int code = connection.getResponseCode();
            if (code == 200) {
                LOGGER.info(jid.toEscapedString() + " is a paying account");
                return true;
            } else {
                LOGGER.info(jid.toEscapedString() + " is not a paying account. code was=" + code);
            }
        } catch (MalformedURLException e) {
            LOGGER.error("unable to create lookup url " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error("unable to lookup paying account");
        }
        return false;
    }
}
