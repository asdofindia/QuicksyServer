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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class VolumeLimiter<E, T> {

    private final Strategy strategy;

    private final LinkedList<VolumeAttempt<E,T>> attempts = new LinkedList<>();

    public VolumeLimiter(Strategy strategy) {
        this.strategy = strategy;
    }

    public synchronized void attempt(E who, List<T> whats) throws RetryInException {

        if (whats.size() == 0) {
            return;
        }

        if (whats.size() > strategy.getAttempts()) {
            throw new RetryInException(Duration.ZERO);
        }

        List<T> remaining = new ArrayList<>(whats);
        final long now = System.nanoTime();


        final Iterator<VolumeAttempt<E,T>> iterator = attempts.iterator();
        int whatsCount = 0;
        VolumeAttempt<E,T> firstAttempt = null;
        while (iterator.hasNext()) {
            VolumeAttempt<E,T> attempt = iterator.next();
            if (attempt.expired(now)) {
                iterator.remove();
            } else {
                if (who.equals(attempt.getWho())) {
                    whatsCount += attempt.getWhats().size();
                    remaining.removeAll(attempt.getWhats());
                    if (firstAttempt == null) {
                        firstAttempt = attempt;
                    }
                    if (remaining.size() == 0) {
                        return;
                    }
                }
            }
        }
        if (whatsCount + remaining.size() > strategy.getAttempts()) {
            throw new RetryInException(firstAttempt == null ? Duration.ZERO : firstAttempt.expiresIn(now));
        }
        attempts.push(new VolumeAttempt<>(who, remaining, strategy.getDuration().toNanos() + now));
    }

    public static class RetryInException extends Exception {
        private final Duration duration;

        private RetryInException(Duration duration) {
            this.duration = duration;
        }

        public Duration getInterval() {
            return duration;
        }

        public String getMessage() {
            return "Retry in "+duration.toString();
        }

    }

}
