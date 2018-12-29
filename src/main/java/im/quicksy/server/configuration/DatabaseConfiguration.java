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

package im.quicksy.server.configuration;

public class DatabaseConfiguration {

    private String url;
    private String username;
    private String password;
    private int poolSize = 1;

    private DatabaseConfiguration() {

    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getJdbcUri() {
        return url;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public static class Builder {

        private final DatabaseConfiguration configuration = new DatabaseConfiguration();

        public DatabaseConfiguration.Builder setUsername(String username) {
            configuration.username = username;
            return this;
        }

        public DatabaseConfiguration.Builder setPassword(String password) {
            configuration.password = password;
            return this;
        }

        public DatabaseConfiguration.Builder setUrl(String url) {
            configuration.url = url;
            return this;
        }

        public DatabaseConfiguration build() {
            return configuration;
        }
    }
}