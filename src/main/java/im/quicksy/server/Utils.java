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

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import im.quicksy.server.configuration.Configuration;
import rocks.xmpp.addr.Jid;

public class Utils {
    static void sleep(long interval) {
        try {
            Thread.sleep(interval);
        } catch (InterruptedException e) {

        }
    }

    public static Jid jidOf(Phonenumber.PhoneNumber phoneNumber) {
        return Jid.of(PhoneNumberUtil.getInstance().format(phoneNumber,PhoneNumberUtil.PhoneNumberFormat.E164), Configuration.getInstance().getDomain(),null);
    }
}
