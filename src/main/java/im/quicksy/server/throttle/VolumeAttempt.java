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

package im.quicksy.server.throttle;

import java.time.Duration;
import java.util.List;

public class VolumeAttempt<E, T> {

    private final List<T> whats;
    private final E who;
    private final long expires;

    public VolumeAttempt(E who, List<T> whats, long expires) {
        this.who = who;
        this.whats = whats;
        this.expires = expires;
    }

    public boolean expired(long now) {
        return expires <= now;
    }

    public Duration expiresIn(long now) {
        return Duration.ofNanos(expires - now);
    }

    public E getWho() {
        return who;
    }

    public List<T> getWhats() {
        return whats;
    }
}
