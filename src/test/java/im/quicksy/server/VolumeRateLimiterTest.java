package im.quicksy.server;

import im.quicksy.server.throttle.Strategy;
import im.quicksy.server.throttle.VolumeLimiter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import rocks.xmpp.addr.Jid;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VolumeRateLimiterTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void hittingMultipleTimesWithSameWhat() throws VolumeLimiter.RetryInException {
        VolumeLimiter<Jid, String> volumeLimiter = new VolumeLimiter<>(
                Strategy.of(Duration.ofHours(1),5)
        );
        Jid personA = Jid.of("person@a.com");
        Jid personB = Jid.of("person@b.com");

        volumeLimiter.attempt(personA, Arrays.asList("1","2","3"));
        volumeLimiter.attempt(personA, Arrays.asList("3","4","5"));
        volumeLimiter.attempt(personA, Arrays.asList("1","2"));

        volumeLimiter.attempt(personB, Arrays.asList("9","8","7"));
        volumeLimiter.attempt(personB, Arrays.asList("7","6","9"));
        volumeLimiter.attempt(personB, Arrays.asList("9","6"));
    }

    @Test
    public void hittingOnceTooLarge() throws VolumeLimiter.RetryInException {
        VolumeLimiter<Jid, String> volumeLimiter = new VolumeLimiter<>(
                Strategy.of(Duration.ofHours(1),5)
        );
        expectedException.expect(VolumeLimiter.RetryInException.class);
        volumeLimiter.attempt(Jid.of("person@domain.tld"),Arrays.asList("1","2","3","4","5","6"));
    }

    @Test
    public void gettingTooLargeOverTime() throws VolumeLimiter.RetryInException {
        VolumeLimiter<Jid, String> volumeLimiter = new VolumeLimiter<>(
                Strategy.of(Duration.ofHours(1),5)
        );
        Jid person = Jid.of("person@a.com");
        volumeLimiter.attempt(person,Arrays.asList("1","2","3","4"));
        volumeLimiter.attempt(person, Collections.singletonList("5"));

        expectedException.expect(VolumeLimiter.RetryInException.class);
        volumeLimiter.attempt(person, Arrays.asList("6","7"));
    }

    @Test
    public void expiry() throws VolumeLimiter.RetryInException, InterruptedException {
        VolumeLimiter<Jid, String> volumeLimiter = new VolumeLimiter<>(
                Strategy.of(Duration.ofSeconds(5), 5)
        );

        Jid personA = Jid.of("person@a.com");
        Jid personB = Jid.of("person@b.com");
        volumeLimiter.attempt(personA, Arrays.asList("1","2","3","4","5"));
        Thread.sleep(1000);
        volumeLimiter.attempt(personB, Arrays.asList("a","b","c","d"));
        Thread.sleep(5000);
        volumeLimiter.attempt(personA, Arrays.asList("6","7","8","9"));
        Thread.sleep(1000);
        volumeLimiter.attempt(personB, Arrays.asList("e","f","g","h","i"));
    }

    @Test
    public void rollingTooFast() throws VolumeLimiter.RetryInException, InterruptedException {
        expectedException.expect(VolumeLimiter.RetryInException.class);
        rolling(500);
    }

    @Test
    public void rollingSlowEnough() throws VolumeLimiter.RetryInException, InterruptedException {
        rolling(1001);
    }

    private void rolling(long timeBetweenAttempts) throws InterruptedException, VolumeLimiter.RetryInException {
        List<Jid> persons = Arrays.asList(Jid.of("a@example.com"),Jid.of("b@example.com"),Jid.of("c@example.com"));
        VolumeLimiter<Jid, String> volumeLimiter = new VolumeLimiter<>(
                Strategy.of(Duration.ofSeconds(5), 5)
        );
        for(int i = 0; i <= 10; ++i) {
            for(Jid jid : persons) {
                volumeLimiter.attempt(jid, Collections.singletonList(String.valueOf(i)));
            }
            if (i != 10) {
                Thread.sleep(timeBetweenAttempts);
            }
        }
    }

}
