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

package im.quicksy.server;

import static spark.Spark.*;

import im.quicksy.server.configuration.Configuration;
import im.quicksy.server.controller.*;
import im.quicksy.server.xmpp.synchronization.Entry;
import im.quicksy.server.xmpp.synchronization.PhoneBook;
import java.io.FileNotFoundException;
import java.util.Properties;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.session.Extension;
import rocks.xmpp.core.session.XmppSessionConfiguration;
import rocks.xmpp.core.session.debug.ConsoleDebugger;
import rocks.xmpp.extensions.bytestreams.s5b.model.Socks5ByteStream;
import rocks.xmpp.extensions.component.accept.ExternalComponent;
import rocks.xmpp.extensions.disco.ServiceDiscoveryManager;
import rocks.xmpp.extensions.disco.model.info.Identity;
import rocks.xmpp.extensions.muc.model.Muc;
import spark.TemplateEngine;
import spark.template.freemarker.FreeMarkerEngine;
import sun.misc.Signal;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final Options options;
    private static final int RETRY_INTERVAL = 5000;

    static {
        options = new Options();
        options.addOption(new Option("c", "config", true, "Path to the config file"));
        options.addOption(new Option("v", "verbose", false, "Set log level to debug"));
        options.addOption(new Option("x", "xmpp", false, "Print stanzas"));
        options.addOption(new Option("h", "help", false, "Show this help"));
    }

    public static void main(String... args) {
        try {
            main(new DefaultParser().parse(options, args));
        } catch (ParseException e) {
            printHelp();
        }
    }

    private static void main(CommandLine commandLine) {
        if (commandLine.hasOption('h')) {
            printHelp();
            return;
        }

        final String configFilename = commandLine.getOptionValue("config");
        if (configFilename != null) {
            try {
                Configuration.setFilename(configFilename);
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
                return;
            }
        }

        if (!Configuration.getInstance().check()) {
            LOGGER.error("Configuration file is incomplete");
            return;
        }

        logConfigurationInfo();

        if (commandLine.hasOption("v")) {
            Properties properties = System.getProperties();
            properties.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        }

        Signal.handle(
                new Signal("HUP"),
                signal -> {
                    try {
                        if (Configuration.reload()) {
                            LOGGER.info("reloaded config");
                            logConfigurationInfo();
                        } else {
                            LOGGER.error("unable to reload config. config file has moved");
                        }
                    } catch (RuntimeException e) {
                        LOGGER.error("Unable to load config file - " + e.getMessage());
                    }
                });

        setupWebServer();
        setupXmppComponent(commandLine.hasOption("x"));
    }

    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar im.quicksy.server-0.1.jar", options);
    }

    private static void logConfigurationInfo() {
        LOGGER.info(
                "validating phone numbers: "
                        + Boolean.toString(Configuration.getInstance().isValidatePhoneNumbers()));
        LOGGER.info(
                "prevent registration when logged in with another device: "
                        + Boolean.toString(Configuration.getInstance().isPreventRegistration()));
        LOGGER.info(
                "minimum client version: "
                        + Configuration.getInstance().getMinVersion().toString());
        LOGGER.info(
                "treat accounts as inactive after: "
                        + Configuration.getInstance().getAccountInactivity());
    }

    private static void setupWebServer() {
        ipAddress(Configuration.getInstance().getWeb().getHost());
        port(Configuration.getInstance().getWeb().getPort());

        final TemplateEngine templateEngine = new FreeMarkerEngine();

        path(
                "/api",
                () -> {
                    get("/", BaseController.index);
                    path(
                            "/password",
                            () -> {
                                before("", BaseController.versionCheck);
                                before("", PasswordController.throttleIp);
                                post("", PasswordController.setPassword);
                            });
                    path(
                            "/authentication",
                            () -> {
                                before("/*", BaseController.versionCheck);
                                before("/*", AuthenticationController.throttleIp);
                                get("/:phoneNumber", AuthenticationController.getAuthentication);
                            });
                });
        path(
                "enter",
                () -> {
                    get("/", EnterController.intro, templateEngine);
                    get(
                            "/send-jabber-verification/",
                            EnterController.getSendJabberVerification,
                            templateEngine);
                    post("/send-jabber-verification/", EnterController.postSendJabberVerification);
                    get("/verify-jabber/", EnterController.getVerifyJabber, templateEngine);
                    post("/verify-jabber/", EnterController.postVerifyJabber);
                    get("/make-payment/", EnterController.getMakePayment, templateEngine);
                    get("/voucher/", EnterController.getVoucher, templateEngine);
                    post("/voucher/", EnterController.postVoucher);
                    get(
                            "/send-number-verification/",
                            EnterController.getSendSmsVerification,
                            templateEngine);
                    post("/send-number-verification/", EnterController.postSendSmsVerification);
                    get("/verify-number/", EnterController.getVerifyNumber, templateEngine);
                    post("/verify-number/", EnterController.postVerifyNumber);
                    get("/finished/", EnterController.getFinished, templateEngine);
                    get("/reset/", EnterController.getReset);
                    get("/confirm-reset/", EnterController.getConfirmReset, templateEngine);
                    get("/confirm-delete/", EnterController.getConfirmDelete, templateEngine);
                    get("/delete/", EnterController.getDelete, templateEngine);
                    post("/checkout/", EnterController.postCheckout);
                    get("/paypal/:status/:uuid/", EnterController.getPayPalResult);
                    get("/payment-received/", EnterController.getPaymentReceived, templateEngine);
                });
    }

    private static void setupXmppComponent(final boolean debug) {
        final XmppSessionConfiguration.Builder builder = XmppSessionConfiguration.builder();
        if (debug) {
            builder.debugger(ConsoleDebugger.class);
        }

        builder.extensions(Extension.of(Entry.class, PhoneBook.class));

        final ExternalComponent externalComponent =
                ExternalComponent.create(
                        Configuration.getInstance().getXmpp().getJid().toEscapedString(),
                        Configuration.getInstance().getXmpp().getSecret(),
                        builder.build(),
                        Configuration.getInstance().getXmpp().getHost(),
                        Configuration.getInstance().getXmpp().getPort());

        ServiceDiscoveryManager serviceDiscoveryManager =
                externalComponent.getManager(ServiceDiscoveryManager.class);
        serviceDiscoveryManager.addFeature(PhoneBook.NAMESPACE);
        serviceDiscoveryManager.addIdentity(Identity.storeGeneric());
        externalComponent.disableFeature(Muc.NAMESPACE);
        externalComponent.disableFeature(Socks5ByteStream.NAMESPACE);

        externalComponent.addIQHandler(
                PhoneBook.class, SynchronizationController.synchronize, true);
        connectAndKeepRetrying(externalComponent);
    }

    private static void connectAndKeepRetrying(final ExternalComponent component) {
        while (true) {
            try {
                component.connect();
                while (component.isConnected()) {
                    Utils.sleep(500);
                }
            } catch (XmppException e) {
                System.err.println(e.getMessage());
            }
            Utils.sleep(RETRY_INTERVAL);
        }
    }
}
