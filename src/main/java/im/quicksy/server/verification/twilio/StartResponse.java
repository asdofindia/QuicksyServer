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

package im.quicksy.server.verification.twilio;

public class StartResponse extends GenericResponse {

    private String carrier;
    private boolean is_cellphone;
    private int seconds_to_expire;
    private String uuid;

    public String getCarrier() {
        return carrier;
    }

    public boolean isIs_cellphone() {
        return is_cellphone;
    }

    public int getSeconds_to_expire() {
        return seconds_to_expire;
    }

    public String getUuid() {
        return uuid;
    }
}
