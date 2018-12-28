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
import java.util.Objects;

public class Strategy {

    private final Duration duration;
    private final int attempts;

    private Strategy(Duration duration, int attempts) {
        this.duration = duration;
        this.attempts = attempts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Strategy strategy = (Strategy) o;
        return attempts == strategy.attempts &&
                Objects.equals(duration, strategy.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration, attempts);
    }

    public static Strategy of(Duration duration, int attempts) {
        return new Strategy(duration, attempts);
    }

    public Duration getDuration() {
        return this.duration;
    }

    public int getAttempts() {
        return attempts;
    }
}
