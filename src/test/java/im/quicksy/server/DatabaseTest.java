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

import im.quicksy.server.configuration.DatabaseConfiguration;
import im.quicksy.server.configuration.DatabaseConfigurationBundle;
import im.quicksy.server.database.Database;
import im.quicksy.server.pojo.Entry;
import im.quicksy.server.pojo.Payment;
import im.quicksy.server.pojo.PaymentMethod;
import org.junit.Test;
import rocks.xmpp.addr.Jid;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class DatabaseTest {

    private static final String JDBC_URL = "jdbc:sqlite::memory:";

    private static final Jid TEST_USER = Jid.of("test@example.com");

    private static final DatabaseConfigurationBundle IN_MEMORY_DATABASE_CONFIGURATION;

    static {
        IN_MEMORY_DATABASE_CONFIGURATION = new DatabaseConfigurationBundle.Builder()
                .setEjabberdConfiguration(new DatabaseConfiguration.Builder()
                        .setUrl(JDBC_URL)
                        .build())
                .setQuicksyConfiguration(new DatabaseConfiguration.Builder()
                        .setUrl(JDBC_URL)
                        .build())
                .build();
    }


    @Test
    public void makePaymentCreateEntityAndReadBack() {
        final Database database = new Database(IN_MEMORY_DATABASE_CONFIGURATION);
        final Payment payment = new Payment(TEST_USER, PaymentMethod.VOUCHER);
        payment.setToken("test");
        database.createPayment(payment);
        assertTrue(database.updatePaymentAndCreateEntry(payment, new Entry(TEST_USER)));

        final Entry entry = database.getEntry(TEST_USER);
        assertNotNull(entry);
    }

    @Test
    public void createEntryAddPhoneNumberAndSearch() {

    }

}
