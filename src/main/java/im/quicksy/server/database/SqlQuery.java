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

public final class SqlQuery {

    static final String FIND_EXISTING_USERS = "select username from users where server_host=:host and username in(:users)";
    static final String FIND_DICTIONARY_ENTRIES = "select jid,phoneNumber from entries where phoneNumber in(:phoneNumbers) and verified=1";

    static final String GET_ENTRY = "SELECT jid,phoneNumber,verified,attempts FROM entries where jid=:jid limit 1";
    static final String CREATE_ENTRY = "insert into entries(jid,phoneNumber,verified,attempts) values(:jid,:phoneNumber,:verified,:attempts)";
    static final String UPDATE_ENTRY = "update entries set phoneNumber=:phoneNumber, verified=:verified, attempts=:attempts where jid=:jid";
    static final String DELETE_ENTRY = "delete from entries where jid=:jid limit 1";

    static final String GET_PAYMENT = "select uuid,owner,method,token,total,status,created from payments where uuid=:uuid limit 1";
    static final String CREATE_PAYMENT = "insert into payments(uuid,owner,method,token,total,status,created) values(:uuid,:owner,:method,:token,:total,:status,:created)";
    static final String MAKE_PAYMENT = "update payments set token=:token,status=:status where uuid=:uuid and status=:expectedStatus";

}
