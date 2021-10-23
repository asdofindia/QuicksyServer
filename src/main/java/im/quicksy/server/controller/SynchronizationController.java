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

import im.quicksy.server.configuration.Configuration;
import im.quicksy.server.database.Database;
import im.quicksy.server.throttle.Strategy;
import im.quicksy.server.throttle.VolumeLimiter;
import im.quicksy.server.xmpp.synchronization.Entry;
import im.quicksy.server.xmpp.synchronization.PhoneBook;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.xmpp.addr.Jid;
import rocks.xmpp.core.stanza.IQHandler;
import rocks.xmpp.core.stanza.model.StanzaError;
import rocks.xmpp.core.stanza.model.errors.Condition;

public class SynchronizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizationController.class);

    private static final VolumeLimiter<Jid, String> PHONE_NUMBER_LIMITER =
            new VolumeLimiter<>(Strategy.of(Duration.ofDays(1), 2048));

    public static IQHandler synchronize =
            iq -> {
                final PhoneBook phoneBook = iq.getExtension(PhoneBook.class);
                final Jid user = iq.getFrom().asBareJid();
                if (phoneBook == null) {
                    return iq.createError(Condition.BAD_REQUEST);
                }
                final String domain = Configuration.getInstance().getDomain();

                if (!user.getDomain().equals(domain)) {
                    return iq.createError(Condition.NOT_AUTHORIZED);
                }

                final List<String> phoneNumbers = phoneBook.getPhoneNumbers();

                try {
                    PHONE_NUMBER_LIMITER.attempt(user, phoneNumbers);
                } catch (VolumeLimiter.RetryInException e) {
                    return iq.createError(
                            new StanzaError(Condition.POLICY_VIOLATION, e.getMessage()));
                }

                LOGGER.info(user + " requested to sync " + phoneNumbers.size() + " phone numbers");
                final HashMap<String, Entry> entryMap = new HashMap<>();
                final List<String> existingUsersOnQuicksy =
                        Database.getInstance().findExistingUsers(domain, phoneNumbers);
                for (String phoneNumber : existingUsersOnQuicksy) {
                    Entry entry = entryMap.computeIfAbsent(phoneNumber, Entry::new);
                    entry.addJid(Jid.of(phoneNumber, domain, null));
                }
                final List<Database.RawEntry> directoryUsers =
                        Database.getInstance().findDirectoryUsers(phoneNumbers);
                for (Database.RawEntry rawEntry : directoryUsers) {
                    Entry entry = entryMap.computeIfAbsent(rawEntry.getPhoneNumber(), Entry::new);
                    entry.addJid(rawEntry.getJid());
                }
                final List<Entry> entries = new ArrayList<>(entryMap.values());
                final String hash = Entry.statusQuo(entries);
                if (hash.equals(phoneBook.getVer())) {
                    LOGGER.info(
                            "hash hasn't changed for "
                                    + user
                                    + " ("
                                    + entries.size()
                                    + " entries)");
                    return iq.createResult();
                }
                ;
                LOGGER.info("responding to " + user + " with " + entries.size() + " entries");
                return iq.createResult(new PhoneBook(entries));
            };
}
