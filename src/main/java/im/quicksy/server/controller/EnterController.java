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

import com.google.common.base.CharMatcher;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import de.gultsch.ejabberd.api.RequestFailedException;
import im.quicksy.server.configuration.Configuration;
import im.quicksy.server.database.Database;
import im.quicksy.server.ejabberd.MyEjabberdApi;
import im.quicksy.server.pojo.Entry;
import im.quicksy.server.pojo.Payment;
import im.quicksy.server.pojo.PaymentMethod;
import im.quicksy.server.pojo.Voucher;
import im.quicksy.server.utils.CimUtils;
import im.quicksy.server.utils.CodeGenerator;
import im.quicksy.server.utils.PayPal;
import im.quicksy.server.verification.NexmoVerificationProvider;
import im.quicksy.server.verification.TwilioVerificationProvider;
import im.quicksy.server.verification.VerificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.xmpp.addr.Jid;
import spark.*;

import java.util.HashMap;

public class EnterController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnterController.class);

    private static final VerificationProvider VERIFICATION_PROVIDER = new TwilioVerificationProvider();

    public static TemplateViewRoute intro = (request, response) -> {
        HashMap<String, Object> model = new HashMap<>();
        model.put("fee", Payment.FEE);
        return new ModelAndView(model, "enter_index.ftl");
    };
    public static TemplateViewRoute getSendJabberVerification = (request, response) -> {
        Session session = request.session();
        session.removeAttribute("jid");
        session.removeAttribute("code");
        session.removeAttribute("attempts");
        HashMap<String, Object> model = new HashMap<>();
        String error = request.queryParams("error");
        if (error != null) {
            model.put("error", error);
        }
        return new ModelAndView(model, "enter_start.ftl");
    };

    public static Route postSendJabberVerification = (request, response) -> {
        String input = request.queryParams("jid");
        if (input != null && !input.isEmpty()) {
            try {
                Jid jid = Jid.of(input);
                if (jid.isDomainJid() || jid.isFullJid()) {
                    response.redirect("/enter/send-jabber-verification/?error=invalid");
                    return "";
                }
                if (jid.getDomain().equals(Configuration.getInstance().getDomain())) {
                    response.redirect("/enter/send-jabber-verification/?error=no-quicksy");
                    return "";
                }
                String from = Configuration.getInstance().getDomain();
                String code = CodeGenerator.generate(6);
                String message = String.format("Your Quicksy verification code is %s", code);
                MyEjabberdApi.getInstance().sendChatMessage(from, jid.asBareJid().toEscapedString(), "Your Quicksy verification code", message);
                final Session session = request.session(true);
                session.attribute("jid", jid.asBareJid());
                session.attribute("code", code);
                session.attribute("attempts", 0);
                response.redirect("/enter/verify-jabber/");
                return "";
            } catch (RequestFailedException e) {
                LOGGER.error("unable to send Jabber verification message - " + e.getMessage());
                response.redirect("/enter/send-jabber-verification/?error=failed");
                return "";
            } catch (IllegalArgumentException e) {
                //fallthrough
            }
        }
        response.redirect("/enter/send-jabber-verification/?error=invalid");
        return "";
    };

    public static TemplateViewRoute getVerifyJabber = (request, response) -> {
        Session session = request.session();
        Jid jid = session.attribute("jid");
        String codeInSession = session.attribute("code");
        if (jid == null || codeInSession == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        HashMap<String, Object> model = new HashMap<>();
        model.put("jid", jid.toEscapedString());
        String error = request.queryParams("error");
        if (error != null) {
            model.put("error", error);
        }
        return new ModelAndView(model, "enter_verify_jid.ftl");
    };
    public static Route postVerifyJabber = (request, response) -> {
        Session session = request.session();
        Jid jid = session.attribute("jid");
        String codeInSession = session.attribute("code");
        Integer attempts = session.attribute("attempts");
        if (jid == null || codeInSession == null || attempts == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        String userEnteredCode = request.queryParams("code");
        if (codeInSession.equals(userEnteredCode)) {
            session.attribute("jid-verified", true);
            session.removeAttribute("code");

            final Entry entry = Database.getInstance().getEntry(jid);
            if (entry == null) {
                if (CimUtils.isPayingAccount(jid)) {
                    if (createEntryAfterSuccessfulVoucher("c.im",jid)) {
                        response.redirect("/enter/send-number-verification/");
                    } else {
                        response.redirect("/enter/make-payment/");
                    }
                } else {
                    response.redirect("/enter/make-payment/");
                }
            } else if (entry.isVerified()) {
                response.redirect("/enter/finished/");
            } else if (entry.getPhoneNumber() != null) {
                response.redirect("/enter/verify-number/");
            } else {
                response.redirect("/enter/send-number-verification/");
            }
        } else {
            attempts++;
            if (attempts >= 5) {
                session.removeAttribute("code");
                response.redirect("/enter/send-jabber-verification/?error=attempts");
            } else {
                session.attribute("attempts", attempts);
                response.redirect("/enter/verify-jabber/?error=invalid");
            }
        }
        return "";
    };
    public static TemplateViewRoute getMakePayment = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        HashMap<String, Object> model = new HashMap<>();
        model.put("jid", jid.toEscapedString());
        model.put("fee", Payment.FEE);
        String error = request.queryParams("error");
        if (error != null) {
            model.put("error", error);
        }
        return new ModelAndView(model, "enter_payment.ftl");
    };
    public static TemplateViewRoute getVoucher = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        return new ModelAndView(null, "enter_voucher.ftl");
    };
    public static Route postVoucher = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        final String voucher = request.queryParams("voucher");
        if (Voucher.checkVoucher(voucher)) {
            if (createEntryAfterSuccessfulVoucher(voucher, jid)) {
                response.redirect("/enter/send-number-verification/");
            } else {
                response.redirect("/enter/make-payment/?error=redeem-voucher");
            }
        } else {
            response.redirect("/enter/make-payment/?error=voucher");
        }
        return null;
    };
    public static TemplateViewRoute getSendSmsVerification = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        final HashMap<String, Object> model = new HashMap<>();
        String error = request.queryParams("error");
        final VerificationProvider.Method method = verificationMethod(request);
        if (error != null) {
            model.put("error", error);
        }
        model.put("method",method);
        return new ModelAndView(model, "enter_phone_number.ftl");
    };

    private static VerificationProvider.Method verificationMethod(Request request) {
        try {
            return VerificationProvider.Method.valueOf(request.queryParamOrDefault("method","SMS"));
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.getMessage());
            return VerificationProvider.Method.SMS;
        }
    }

    public static Route postSendSmsVerification = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        final String input = request.queryParams("number");
        final VerificationProvider.Method method = verificationMethod(request);
        final String number = input == null ? "" : CharMatcher.whitespace().removeFrom(input);
        if (E164_PATTERN.matcher(number).matches()) {
            try {
                Phonenumber.PhoneNumber phoneNumber = PhoneNumberUtil.getInstance().parse(number, "us");
                if (Configuration.getInstance().isValidatePhoneNumbers() && !PhoneNumberUtil.getInstance().isValidNumber(phoneNumber)) {
                    LOGGER.info("libphonenumber reported " + phoneNumber + " as invalid");
                    response.redirect("/enter/send-number-verification/?error=invalid");
                    return null;
                }
                final Entry entry = Database.getInstance().getEntry(jid);
                if (entry == null) {
                    response.redirect("/enter/make-payment/?error=not-yet");
                    return null;
                }

                if (entry.isVerified() && entry.getPhoneNumber() != null) {
                    response.redirect("/enter/send-number-verification/?error=already");
                    return null;
                }

                if (entry.getAttempts() <= 0) {
                    response.redirect("/enter/send-number-verification/?error=attempts");
                    return null;
                }

                try {
                    VERIFICATION_PROVIDER.request(phoneNumber, method);
                } catch (im.quicksy.server.verification.RequestFailedException e) {
                    if (e.getCode() == TwilioVerificationProvider.PHONE_NUMBER_IS_INVALID) {
                        LOGGER.info("verification provider said " + phoneNumber + " is invalid");
                        response.redirect("/enter/send-number-verification/?error=invalid");
                        return null;
                    }
                    LOGGER.warn("unable to send SMS verification message to " + phoneNumber + " (" + e.getMessage() + ", code=" + e.getCode() + ")");
                    response.redirect("/enter/send-number-verification/?error=failed");
                    return null;
                }
                entry.setPhoneNumber(phoneNumber);
                entry.setVerified(false);
                entry.decreaseAttempts();
                Database.getInstance().updateEntry(entry);
                response.redirect("/enter/verify-number/?method="+method.toString());
            } catch (NumberParseException e) {
                response.redirect("/enter/send-number-verification/?error=invalid");
            }
        } else {
            response.redirect("/enter/send-number-verification/?error=invalid");
        }
        return null;
    };
    public static TemplateViewRoute getVerifyNumber = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        final HashMap<String, Object> model = new HashMap<>();

        final Entry entry = Database.getInstance().getEntry(jid);

        if (entry == null) {
            response.status(500);
            return null;
        } else if (entry.getPhoneNumber() == null) {
            response.redirect("/enter/send-number-verification/");
            return null;
        }

        final String error = request.queryParams("error");
        if (error != null) {
            model.put("error", error);
        }
        model.put("attempts", entry.getAttempts());
        model.put("number", PhoneNumberUtil.getInstance().format(entry.getPhoneNumber(), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
        model.put("method",verificationMethod(request));
        return new ModelAndView(model, "enter_verify_number.ftl");
    };
    public static Route postVerifyNumber = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        final Entry entry = Database.getInstance().getEntry(jid);

        if (entry == null) {
            response.redirect("/enter/make-payment/?error=not-yet");
            return null;
        }

        if (entry.isVerified() && entry.getPhoneNumber() != null) {
            response.redirect("/enter/finished/");
            return null;
        }

        String code = request.queryParams("code");
        try {
            if (code == null || code.trim().isEmpty() || !VERIFICATION_PROVIDER.verify(entry.getPhoneNumber(), code.trim())) {
                response.redirect("/enter/verify-number/?error=invalid");
            } else {
                entry.setVerified(true);
                Database.getInstance().updateEntry(entry);
                response.redirect("/enter/finished/");
            }
        } catch (im.quicksy.server.verification.RequestFailedException e) {
            if (e.getCode() == TwilioVerificationProvider.PHONE_VERIFICATION_NOT_FOUND) {
                response.redirect("/enter/verify-number/?error=expired");
            } else {
                LOGGER.error("problem verifying sms authentication token"+e.getMessage()+" code="+e.getCode());
                response.redirect("/enter/verify-number/?error=upstream");
            }
        }
        return null;
    };
    public static TemplateViewRoute getFinished = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        final HashMap<String, Object> model = new HashMap<>();

        final Entry entry = Database.getInstance().getEntry(jid);

        session.removeAttribute("delete-confirmed");
        session.removeAttribute("reset-confirmed");

        if (entry == null || !entry.isVerified() || entry.getPhoneNumber() == null) {
            response.status(500);
            return null;
        }

        model.put("number", PhoneNumberUtil.getInstance().format(entry.getPhoneNumber(), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
        model.put("jid", jid.toEscapedString());
        model.put("attempts", entry.getAttempts());
        return new ModelAndView(model, "enter_finished.ftl");
    };
    public static Route getReset = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        Entry entry = Database.getInstance().getEntry(jid);
        if (entry != null) {
            Boolean resetConfirmed = session.attribute("reset-confirmed");
            if (entry.isVerified() && (resetConfirmed == null || !resetConfirmed)) {
                response.redirect("/enter/finished/");
            } else if (entry.getAttempts() <= 0) {
                response.redirect("/enter/verify-number/");
            } else {
                entry.setPhoneNumber(null);
                entry.setVerified(false);
                Database.getInstance().updateEntry(entry);
                VerificationProvider.Method method = verificationMethod(request);
                response.redirect("/enter/send-number-verification/?method="+method.toString());
            }
        } else {
            response.redirect("/enter/make-payment/");
        }
        return null;
    };

    public static TemplateViewRoute getConfirmReset = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        final HashMap<String, Object> model = new HashMap<>();
        Entry entry = Database.getInstance().getEntry(jid);
        if (entry != null) {
            if (entry.isVerified() && entry.getAttempts() <= 0) {
                response.redirect("/enter/finished/");
                return null;
            } else {
                session.attribute("reset-confirmed",true);
            }
        } else {
            response.redirect("/enter/make-payment/");
            return null;
        }
        return new ModelAndView(model, "confirm_reset.ftl");
    };

    public static TemplateViewRoute getConfirmDelete = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        session.attribute("delete-confirmed",true);
        return new ModelAndView(new HashMap<>(), "confirm_delete.ftl");
    };

    public static TemplateViewRoute getDelete = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        final Boolean deleteConfirmed = session.attribute("delete-confirmed");
        final Entry entry = Database.getInstance().getEntry(jid);
        if (deleteConfirmed == null || !deleteConfirmed) {
            if (entry != null && entry.isVerified()) {
                response.redirect("/enter/finished/");
                return null;
            } else {
                response.redirect("/enter/send-jabber-verification/?error=cookies");
                return null;
            }
        }
        if (entry == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        Database.getInstance().deleteEntry(entry);
        session.invalidate();
        return new ModelAndView(new HashMap<>(), "deleted.ftl");
    };

    public static Route getPayPalResult = (request, response) -> {
        final String uuid = request.params("uuid");
        final boolean success = "success".equals(request.params("status"));
        String token = request.queryParams("token");
        String payerId = request.queryParams("PayerID");
        if (token == null || uuid == null || !success) {
            response.redirect("/enter/make-payment/?error=failed");
            return null;
        }
        Payment payment = Database.getInstance().getPayment(uuid);
        if (payment == null) {
            response.redirect("/enter/make-payment/?error=cart");
            return null;
        }
        if (PayPal.doExpressCheckoutPayment(token, payerId, payment.getTotal())) {
            if (Database.getInstance().updatePaymentAndCreateEntry(payment, new Entry(payment.getOwner()))) {
                response.redirect("/enter/payment-received/");
            } else {
                response.redirect("/enter/make-payment/?error=redeem-failed");
            }
        } else {
            response.redirect("/enter/make-payment/?error=failed");
        }
        return null;
    };
    public static Route postCheckout = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        Payment payment = new Payment(jid, PaymentMethod.PAYPAL);
        final String token = PayPal.setExpressCheckout(payment);
        payment.setToken(token);
        Database.getInstance().createPayment(payment);
        response.redirect(PayPal.getRedirectionUrl(token));
        return null;
    };
    public static TemplateViewRoute getPaymentReceived = (request, response) -> {
        final Session session = request.session();
        final Jid jid = getVerifiedJid(session);
        if (jid == null) {
            response.redirect("/enter/send-jabber-verification/?error=cookies");
            return null;
        }
        return new ModelAndView(null, "enter_payment_received.ftl");
    };

    private static Jid getVerifiedJid(Session session) {
        final Jid jid = session.attribute("jid");
        final Boolean verified = session.attribute("jid-verified");
        if (jid != null && verified != null && verified) {
            return jid;
        }
        session.invalidate();
        return null;
    }

    private static boolean createEntryAfterSuccessfulVoucher(String code, Jid jid) {
        Payment payment = new Payment(jid, PaymentMethod.VOUCHER);
        payment.setToken(code);
        Database.getInstance().createPayment(payment);
        return Database.getInstance().updatePaymentAndCreateEntry(payment, new Entry(jid));
    }
}
