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

package im.quicksy.server.database;

import com.google.i18n.phonenumbers.Phonenumber;
import com.zaxxer.hikari.HikariDataSource;
import de.gultsch.xmpp.addr.adapter.Adapter;
import im.quicksy.server.Configuration;
import im.quicksy.server.pojo.Entry;
import im.quicksy.server.pojo.Payment;
import im.quicksy.server.pojo.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.converters.Converter;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.Quirks;
import rocks.xmpp.addr.Jid;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static im.quicksy.server.database.SqlQuery.*;

public class Database {

    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    private static final Database INSTANCE = new Database();
    private final Sql2o ejabberdDatabase;
    private final Sql2o quicksyDatabase;

    private Database() {
        HashMap<Class, Converter> converters = new HashMap<>();
        Adapter.register(converters);
        converters.put(Phonenumber.PhoneNumber.class, new PhoneNumberConverter());
        converters.put(LocalDateTime.class, new LocalDateTimeConverter());
        converters.put(UUID.class, new UUIDStringConverter());
        Quirks quirks = new NoQuirks(converters);
        this.ejabberdDatabase = createDatabase("ejabberd", quirks);
        this.quicksyDatabase = createDatabase("quicksy", quirks);
    }

    private static Sql2o createDatabase(String database, Quirks quirks) {
        HikariDataSource dataSource = new HikariDataSource();
        Configuration.DB dbConfig = Configuration.getInstance().getDb();
        dataSource.setMaximumPoolSize(dbConfig.getPoolSize());
        dataSource.setJdbcUrl(dbConfig.getJdbcUri(database));
        dataSource.setUsername(dbConfig.getUsername());
        dataSource.setPassword(dbConfig.getPassword());
        return new Sql2o(dataSource, quirks);
    }

    public static Database getInstance() {
        return INSTANCE;
    }

    public List<String> findExistingUsers(String host, List<String> users) {
        try (Connection connection = this.ejabberdDatabase.open()) {
            return connection.createQuery(FIND_EXISTING_USERS)
                    .addParameter("host", host)
                    .addParameter("users", users)
                    .executeAndFetch(String.class);
        }
    }

    public List<RawEntry> findDirectoryUsers(List<String> phoneNumbers) {
        try (Connection connection = this.quicksyDatabase.open()) {
            return connection.createQuery(FIND_DICTIONARY_ENTRIES)
                    .addParameter("phoneNumbers", phoneNumbers)
                    .executeAndFetch(RawEntry.class);
        }
    }

    public Entry getEntry(Jid jid) {
        try (Connection connection = this.quicksyDatabase.open()) {
            return connection.createQuery(GET_ENTRY).addParameter("jid", jid).executeAndFetchFirst(Entry.class);
        }
    }

    public void updateEntry(final Entry entry) {
        try (Connection connection = this.quicksyDatabase.open()) {
            connection.createQuery(UPDATE_ENTRY).bind(entry).executeUpdate();
        }
    }

    public void deleteEntry(final Entry entry) {
        try (Connection connection = this.quicksyDatabase.open()) {
            connection.createQuery(DELETE_ENTRY).bind(entry).executeUpdate();
        }
    }

    public Payment getPayment(String uuid) {
        try (Connection connection = this.quicksyDatabase.open()) {
            return connection.createQuery(GET_PAYMENT).addParameter("uuid", uuid).executeAndFetchFirst(Payment.class);
        }
    }

    public void createPayment(Payment payment) {
        try (Connection connection = this.quicksyDatabase.open()) {
            connection.createQuery(CREATE_PAYMENT).bind(payment).executeUpdate();
        }
    }

    public boolean updatePaymentAndCreateEntry(Payment payment, Entry entry) {
        try (Connection connection = this.quicksyDatabase.beginTransaction()) {
            connection.setRollbackOnException(true);
            connection.createQuery(CREATE_ENTRY).bind(entry).executeUpdate();
            int count = connection.createQuery(MAKE_PAYMENT)
                    .addParameter("status", PaymentStatus.PAYED)
                    .addParameter("expectedStatus",PaymentStatus.PENDING)
                    .addParameter("uuid", UUID.class, payment.getUuid()) //uuid somehow needs to be called with class to run through the converter
                    .addParameter("token", payment.getToken()).executeUpdate().getResult();
            if (count != 1) {
                throw new Exception("Unable to make payment. updated row count was "+count);
            }
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            return false;
        }
        return true;
    }

    public static class RawEntry {
        private Jid jid;
        private String phoneNumber;

        public Jid getJid() {
            return jid;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }
    }
}
