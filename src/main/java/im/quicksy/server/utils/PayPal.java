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

import im.quicksy.server.configuration.Configuration;
import im.quicksy.server.pojo.Payment;
import im.quicksy.server.pojo.ShoppingCartItem;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

public class PayPal {

    private static final String API_URL = "https://api-3t.paypal.com/nvp";

    private static final int VERSION = 93;

    private static final String ENCODING = "UTF-8";

    private static final String REDIRECTION_URL =
            "https://www.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=";

    private static DecimalFormat CURRENCY_FORMAT = new DecimalFormat("0.00");

    private static final String CALLBACK_URL_BASE = "https://quicksy.im/enter/paypal/";

    public static String setExpressCheckout(Payment payment) throws PayPalAPIException {
        HashMap<String, String> params = new HashMap<>();
        params.put(
                "PAYMENTREQUEST_0_AMT",
                CURRENCY_FORMAT.format(payment.getTotal())); // CURRENCY_FORMAT.format(amount));
        params.put("PAYMENTREQUEST_0_ITEMAMT", CURRENCY_FORMAT.format(payment.getTotal()));
        int i = 0;
        for (ShoppingCartItem item : payment.getItems()) {
            params.put("L_PAYMENTREQUEST_0_NAME" + Integer.toString(i), item.getDescription());
            params.put("L_PAYMENTREQUEST_0_QTY" + Integer.toString(i), "1");
            params.put(
                    "L_PAYMENTREQUEST_0_AMT0" + Integer.toString(i),
                    CURRENCY_FORMAT.format(item.getPrice()));
            ++i;
        }
        params.put("PAYMENTREQUEST_0_CURRENCYCODE", "EUR");
        params.put("PAYMENTREQUEST_0_PAYMENTACTION", "SALE");
        params.put("NOSHIPPING", "1");
        params.put("BRANDNAME", "Quicksy");
        params.put("cancelUrl", CALLBACK_URL_BASE + "cancel/" + payment.getUuid().toString() + "/");
        params.put(
                "returnUrl", CALLBACK_URL_BASE + "success/" + payment.getUuid().toString() + "/");
        try {
            HashMap<String, String> result = executeApiCall("SetExpressCheckout", params);
            System.out.println(result);
            String token = result.get("TOKEN");
            String ack = result.get("ACK");
            if (!"Success".equals(ack)) {
                throw new PayPalAPIException("call was unsuccessful");
            }
            return token;
        } catch (Exception e) {
            throw new PayPalAPIException(e.getMessage());
        }
    }

    public static HashMap<String, String> getExpressCheckoutDetails(String token)
            throws PayPalAPIException {
        HashMap<String, String> params = new HashMap<>();
        params.put("TOKEN", token);
        try {
            return executeApiCall("GetExpressCheckoutDetails", params);
        } catch (Exception e) {
            throw new PayPalAPIException(e.getMessage());
        }
    }

    public static boolean doExpressCheckoutPayment(String token, String payerId, double total)
            throws PayPalAPIException {
        HashMap<String, String> params = new HashMap<>();
        params.put("TOKEN", token);
        params.put("PAYERID", payerId);
        params.put("PAYMENTREQUEST_0_AMT", CURRENCY_FORMAT.format(total));
        params.put("PAYMENTREQUEST_0_CURRENCYCODE", "EUR");
        params.put("PAYMENTREQUEST_0_PAYMENTACTION", "SALE");
        try {
            HashMap<String, String> result = executeApiCall("DoExpressCheckoutPayment", params);
            System.out.println("result from doExpressCheckoutPayment" + result);
            return "Success".equals(result.get("ACK"));
        } catch (Exception e) {
            throw new PayPalAPIException(e.getMessage());
        }
    }

    private static HashMap<String, String> executeApiCall(
            String method, HashMap<String, String> params) throws Exception {
        Configuration.PayPal config = Configuration.getInstance().getPayPal();
        params.put("METHOD", method);
        params.put("USER", config.getUsername());
        params.put("PWD", config.getPassword());
        params.put("SIGNATURE", config.getSignature());
        params.put("VERSION", String.valueOf(VERSION));
        return executeApiCall(params);
    }

    private static HashMap<String, String> executeApiCall(HashMap<String, String> params)
            throws Exception {
        final URL url = new URL(API_URL);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (postData.length() != 0) {
                postData.append('&');
            }
            postData.append(URLEncoder.encode(param.getKey(), ENCODING));
            postData.append("=");
            postData.append(URLEncoder.encode(param.getValue(), ENCODING));
        }
        System.out.println("post data " + postData.toString());
        byte[] postDataBytes = postData.toString().getBytes(ENCODING);
        writer.write(postDataBytes);
        HashMap<String, String> result = new HashMap<>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder builder = new StringBuilder();
        for (int c; (c = reader.read()) >= 0; ) {
            builder.append((char) c);
        }
        for (String pair : builder.toString().split("&")) {
            String[] parts = pair.split("=", 2);
            result.put(
                    URLDecoder.decode(parts[0], ENCODING), URLDecoder.decode(parts[1], ENCODING));
        }
        return result;
    }

    public static String getRedirectionUrl(String token) {
        return REDIRECTION_URL + token;
    }

    private static class PayPalAPIException extends Exception {

        PayPalAPIException(String message) {
            super(message);
        }
    }
}
