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

package im.quicksy.server.configuration;


import com.github.zafarkhaja.semver.Version;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import de.gultsch.xmpp.addr.adapter.Adapter;
import im.quicksy.server.json.DurationDeserializer;
import im.quicksy.server.json.VersionDeserializer;
import rocks.xmpp.addr.Jid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.Duration;
import java.util.*;

public class Configuration {

    private static File FILE = new File("config.json");
    private static Configuration INSTANCE;

    private XMPP xmpp = new XMPP();
    private Web web = new Web();
    private HashMap<String, DatabaseConfiguration> db;
    private PayPal payPal = new PayPal();

    private TreeMap<String, ProviderConfiguration> provider;

    private String cimAuthToken;
    private Version minVersion;
    private Duration accountInactivity = Duration.ofDays(28);
    private String domain;
    private boolean validatePhoneNumbers = true;
    private boolean preventRegistration = true;

    private Configuration() {

    }

    public synchronized static void setFilename(String filename) throws FileNotFoundException {
        if (INSTANCE != null) {
            throw new IllegalStateException("Unable to set filename after instance has been created");
        }
        Configuration.FILE = new File(filename);
        if (!Configuration.FILE.exists()) {
            throw new FileNotFoundException();
        }
    }

    public synchronized static Configuration getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    private static Configuration load() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Adapter.register(gsonBuilder);
        gsonBuilder.registerTypeAdapter(Version.class, new VersionDeserializer());
        gsonBuilder.registerTypeAdapter(Duration.class, new DurationDeserializer());
        final Gson gson = gsonBuilder.create();
        try {
            System.out.println("Reading configuration from " + FILE.getAbsolutePath());
            return gson.fromJson(new FileReader(FILE), Configuration.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Configuration file not found");
        } catch (JsonParseException e) {
            throw new RuntimeException("Unable to parse configuration file", e);
        }
    }

    public synchronized static boolean reload() {
        if (Configuration.FILE.exists()) {
            final Configuration newConfig = load();
            if (!newConfig.check()) {
                throw new RuntimeException("Configuration file is incomplete");
            }
            INSTANCE = newConfig;
            return true;
        } else {
            return false;
        }
    }

    public boolean check() {
        return domain != null && minVersion != null && web != null && xmpp != null && xmpp.check() && db != null && db.size() == 2 && payPal != null && payPal.check();
    }

    public Duration getAccountInactivity() {
        return accountInactivity;
    }

    public boolean isPreventRegistration() {
        return preventRegistration;
    }

    public boolean isValidatePhoneNumbers() {
        return validatePhoneNumbers;
    }

    public XMPP getXmpp() {
        return xmpp;
    }

    public Web getWeb() {
        return web;
    }

    public DatabaseConfigurationBundle getDatabaseConfigurationBundle() {
        return new DatabaseConfigurationBundle.Builder().setEjabberdConfiguration(db.get("ejabberd")).setQuicksyConfiguration(db.get("quicksy")).build();
    }

    public Optional<String> getCimAuthToken() {
        return Optional.ofNullable(cimAuthToken);
    }

    public String getDomain() {
        return domain;
    }

    public PayPal getPayPal() {
        return payPal;
    }

    public File getVoucherFile() {
        return new File(FILE.getParentFile(), "vouchers.json");
    }

    public Version getMinVersion() {
        return minVersion;
    }

    public TreeMap<String, Configuration.ProviderConfiguration> getProvider() {
        return this.provider;
    }

    public static class XMPP {
        private String host = "localhost";
        private int port = 5347;
        private Jid jid;
        private String secret;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public Jid getJid() {
            return jid;
        }

        public String getSecret() {
            return secret;
        }

        public boolean check() {
            return secret != null && jid != null;
        }
    }

    public static class Web {
        private String host = "127.0.0.1";
        private int port = 4567;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }

    public static class PayPal {
        private String username;
        private String password;
        private String signature;

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getSignature() {
            return signature;
        }

        public boolean check() {
            return username != null && password != null && signature != null;
        }
    }


    public static class ProviderConfiguration {
        private Map<String, String> parameter;
        private List<Integer> deny;

        public Map<String, String> getParameter() {
            return parameter;
        }

        public List<Integer> getDeny() {
            return deny;
        }
    }
}
