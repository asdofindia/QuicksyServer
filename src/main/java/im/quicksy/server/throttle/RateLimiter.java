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
import java.util.*;

public class RateLimiter<T> {

    private final HashMap<Strategy, LinkedList<Attempt<T>>> storage = new HashMap<>();

    private final Strategy[] strategies;

    private RateLimiter(Strategy[] strategies) {
        Arrays.sort(strategies, (a, b) -> b.getDuration().compareTo(a.getDuration()));
        this.strategies = strategies;
    }

    public static <T> RateLimiter<T> of(Strategy... strategies) {
        return new RateLimiter<T>(strategies);
    }

    public synchronized void attempt(final T entity) throws RetryInException {
        final long now = System.nanoTime();
        check(entity, now);
        for (Strategy strategy : strategies) {
            storage.computeIfAbsent(strategy, k -> new LinkedList<>())
                    .addLast(Attempt.create(entity, strategy.getDuration().toNanos() + now));
        }
    }

    private void check(T entity, final long now) throws RetryInException {
        for (Strategy strategy : strategies) {
            final LinkedList<Attempt<T>> list =
                    storage.computeIfAbsent(strategy, k -> new LinkedList<>());
            final Iterator<Attempt<T>> iterator = list.iterator();
            int entityCount = 0;
            Attempt<T> firstAttempt = null;
            while (iterator.hasNext()) {
                Attempt<T> attempt = iterator.next();
                if (attempt.expired(now)) {
                    iterator.remove();
                } else {
                    if (entity.equals(attempt.getEntity())) {
                        ++entityCount;
                        if (firstAttempt == null) {
                            firstAttempt = attempt;
                        }
                        if (entityCount >= strategy.getAttempts()) {
                            throw new RetryInException(firstAttempt.expiresIn(now), entity);
                        }
                    }
                }
            }
        }
    }

    public static class RetryInException extends Exception {
        private final Duration duration;
        private final Object object;

        private RetryInException(Duration duration, Object entity) {
            this.duration = duration;
            this.object = entity;
        }

        public Duration getInterval() {
            return duration;
        }

        public String getEntityType() {
            return object.getClass().getSimpleName();
        }

        @Override
        public String getMessage() {
            return "Throttled "
                    + getEntityType()
                    + "("
                    + this.object.toString()
                    + "). Retry in "
                    + duration.toString();
        }
    }
}
