package im.quicksy.server;

import im.quicksy.server.verification.NexmoVerificationProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NexmoVerificationProviderTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void pinExpiry() {
        NexmoVerificationProvider.Pin pin = NexmoVerificationProvider.Pin.generate();
        System.out.println(pin);
        pin.verify("000000");
        pin.verify("000000");
        pin.verify("000000");
        expectedException.expect(NexmoVerificationProvider.TooManyAttemptsException.class);
        pin.verify("000000");
    }
}
