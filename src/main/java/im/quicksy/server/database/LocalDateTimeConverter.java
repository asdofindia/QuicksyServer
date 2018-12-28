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

import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class LocalDateTimeConverter implements Converter<LocalDateTime> {

    @Override
    public LocalDateTime convert(Object o) throws ConverterException {
        if (o instanceof Timestamp) {
            return ((Timestamp) o).toLocalDateTime();
        } else {
            throw new ConverterException("can not convert object of type " + o.getClass().getName() + " to LocalDateTime");
        }
    }

    @Override
    public Object toDatabaseParam(LocalDateTime localDateTime) {
        return Timestamp.valueOf(localDateTime);
    }
}