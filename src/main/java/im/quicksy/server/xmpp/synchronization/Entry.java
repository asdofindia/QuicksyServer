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

package im.quicksy.server.xmpp.synchronization;

import com.google.common.io.BaseEncoding;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import rocks.xmpp.addr.Jid;

@XmlRootElement
public class Entry implements Comparable<Entry> {

    @XmlAttribute private String number;

    @XmlElement(name = "jid", namespace = PhoneBook.NAMESPACE)
    private List<Jid> jids;

    private Entry() {}

    public Entry(String number) {
        this.number = number;
        this.jids = new ArrayList<>();
    }

    public Entry(String number, Jid jid) {
        this.number = number;
        this.jids = Collections.singletonList(jid);
    }

    public String getNumber() {
        return number;
    }

    public List<Jid> getJids() {
        return jids;
    }

    public static String statusQuo(final List<Entry> entries) {
        Collections.sort(entries);
        StringBuilder builder = new StringBuilder();
        for (Entry entry : entries) {
            if (builder.length() != 0) {
                builder.append('\u001d');
            }
            builder.append(entry.getNumber());
            List<Jid> jids = entry.getJids();
            Collections.sort(jids);
            for (Jid jid : jids) {
                builder.append('\u001e');
                builder.append(jid.asBareJid().toEscapedString());
            }
        }
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
        byte[] sha1 = md.digest(builder.toString().getBytes());
        return BaseEncoding.base64().encode(sha1);
    }

    @Override
    public int compareTo(Entry entry) {
        return number.compareTo(entry.number);
    }

    public void addJid(Jid jid) {
        this.jids.add(jid);
    }
}
