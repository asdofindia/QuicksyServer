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

package im.quicksy.server.pojo;

import com.google.i18n.phonenumbers.Phonenumber;
import rocks.xmpp.addr.Jid;

public class Entry {


    private static final int DEFAULT_ATTEMPTS = 3;

    private Jid jid;
    private Phonenumber.PhoneNumber phoneNumber;
    private boolean verified;

    public Jid getJid() {
        return jid;
    }

    public Phonenumber.PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(Phonenumber.PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    private int attempts;

    public Entry(Jid jid) {
        this.jid = jid;
        this.attempts = DEFAULT_ATTEMPTS;
        this.verified = false;
    }

    public void decreaseAttempts() {
        this.attempts--;
    }
}
