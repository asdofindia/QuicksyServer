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

import im.quicksy.server.controller.BaseController;

import javax.xml.bind.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement(name = "phone-book", namespace = PhoneBook.NAMESPACE)
public class PhoneBook {

    public static final String NAMESPACE = "im.quicksy.synchronization:0";

    @XmlAttribute
    private String ver;

    @XmlElement(name="entry")
    private List<Entry> entries;

    private PhoneBook() {

    }

    public PhoneBook(List<Entry> entries) {
        this.entries = entries;
    }

    public List<String> getPhoneNumbers() {
        return entries == null ? Collections.emptyList() : entries.stream().map(Entry::getNumber).filter(n -> BaseController.E164_PATTERN.matcher(n).matches()).collect(Collectors.toList());
    }

    public String getVer() {
        return ver;
    }
}
