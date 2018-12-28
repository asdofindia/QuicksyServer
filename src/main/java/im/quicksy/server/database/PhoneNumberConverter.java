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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;

import java.util.regex.Pattern;

public class PhoneNumberConverter implements Converter<PhoneNumber> {

    public static Pattern E164_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");

    @Override
    public PhoneNumber convert(Object o) throws ConverterException {
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            String string = (String) o;
            if (!E164_PATTERN.matcher(string).matches()) {
                throw new ConverterException("String doesn't match e164 pattern");
            }
            try {
                return PhoneNumberUtil.getInstance().parse(string,"us");
            } catch (NumberParseException e) {
                throw new ConverterException(e.getMessage());
            }
        } else {
            throw new ConverterException("Can only convert strings");
        }
    }

    @Override
    public Object toDatabaseParam(PhoneNumber phoneNumber) {
        return PhoneNumberUtil.getInstance().format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    }
}
