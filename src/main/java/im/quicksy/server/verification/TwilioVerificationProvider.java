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

package im.quicksy.server.verification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.i18n.phonenumbers.Phonenumber;
import im.quicksy.server.configuration.Configuration;
import im.quicksy.server.verification.twilio.ErrorResponse;
import im.quicksy.server.verification.twilio.GenericResponse;
import im.quicksy.server.verification.twilio.StartResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class TwilioVerificationProvider implements VerificationProvider {


    public static final int PHONE_VERIFICATION_INCORRECT = 60022;
    public static final int PHONE_VERIFICATION_NOT_FOUND = 60023;
    public static final int PHONE_NUMBER_IS_INVALID = 60033;
    public static final int PHONE_NUMBER_IS_NOT_A_VALID_MOBILE_NUMBER = 21614;

    private static final String TWILIO_API_URL = "https://api.authy.com/protected/json/phones/verification/";
    private static final Logger LOGGER = LoggerFactory.getLogger(TwilioVerificationProvider.class);
    private final GsonBuilder gsonBuilder = new GsonBuilder();

    @Override
    public boolean verify(Phonenumber.PhoneNumber phoneNumber, String pin) throws RequestFailedException {
        Map<String, String> params = new HashMap<>();
        params.put("phone_number", Long.toString(phoneNumber.getNationalNumber()));
        params.put("country_code", Integer.toString(phoneNumber.getCountryCode()));
        params.put("verification_code", pin);
        try {
            final String method = "check?" + getQuery(params);
            final GenericResponse response = execute(method, null, GenericResponse.class);
            LOGGER.info("twilio message was " + response.getMessage());
            if (response.isSuccess()) {
                return true;
            } else {
                throw new RequestFailedException(response.getMessage());
            }
        } catch (RequestFailedException e) {
            if (e.getCode() == PHONE_VERIFICATION_INCORRECT) {
                return false;
            }
            throw e;
        } catch (UnsupportedEncodingException e) {
            throw new RequestFailedException(e);
        }
    }

    @Override
    public void request(Phonenumber.PhoneNumber phoneNumber, Method method) throws RequestFailedException {
        request(phoneNumber, method, null);
    }

    @Override
    public void request(Phonenumber.PhoneNumber phoneNumber, Method method, String language) throws RequestFailedException {
        LOGGER.info("requesting verification ("+method.toString()+") for " + phoneNumber);
        Map<String, String> params = new HashMap<>();
        params.put("via", method.toString().toLowerCase(Locale.ENGLISH));
        params.put("phone_number", Long.toString(phoneNumber.getNationalNumber()));
        params.put("country_code", Integer.toString(phoneNumber.getCountryCode()));
        params.put("code_length", Integer.toString(6));
        if (language != null) {
            params.put("locale", "en");
        }
        try {
            final StartResponse response = execute("start", params, StartResponse.class);
            if (!response.isSuccess()) {
                throw new RequestFailedException(response.getMessage());
            }
        } catch (RequestFailedException e) {
            throw e;
        }
    }

    private String getQuery(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> param : params.entrySet()) {

            if (result.length() != 0) {
                result.append('&');
            }

            result.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(param.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    private <T> T execute(final String method, final Map<String, String> params, Class<T> clazz) throws RequestFailedException {
        String result = null;
        try {
            final Gson gson = this.gsonBuilder.create();
            final HttpURLConnection connection = (HttpURLConnection) new URL(TWILIO_API_URL + method).openConnection();
            connection.setRequestProperty("X-Authy-API-Key", Configuration.getInstance().getTwilioAuthToken());
            if (params != null && params.size() > 0) {
                connection.setRequestMethod("POST");
                final String output = getQuery(params);
                connection.setDoOutput(true);
                OutputStreamWriter outputStream = new OutputStreamWriter(connection.getOutputStream());
                outputStream.write(output);
                outputStream.flush();
                outputStream.close();
            }
            int code = connection.getResponseCode();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(code == 200 ? connection.getInputStream() : connection.getErrorStream()));
            result = bufferedReader.lines().collect(Collectors.joining("\n"));
            if (code == 200) {
                return gson.fromJson(result, clazz);
            } else {
                LOGGER.debug("json was " + result);
                ErrorResponse error = gson.fromJson(result, ErrorResponse.class);
                throw new RequestFailedException(error.getMessage(), error.getErrorCode());
            }
        } catch (JsonSyntaxException e) {
            final String firstLine = result == null ? "" : result.split("\n")[0];
            throw new RequestFailedException("Unable to parse JSON starting with " + firstLine.substring(0, Math.min(firstLine.length(), 20)), e);
        } catch (RequestFailedException e) {
            throw e;
        } catch (Throwable t) {
            throw new RequestFailedException(t);
        }
    }
}
