package fr.xephi.authme.util.expiring;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link Duration}.
 */
public class DurationTest {

    @Test
    public void shouldConvertToAppropriateTimeUnit() {
        check(Duration.createWithSuitableUnit(0, TimeUnit.HOURS),
            0, TimeUnit.SECONDS);

        check(Duration.createWithSuitableUnit(124, TimeUnit.MINUTES),
            2, TimeUnit.HOURS);

        check(Duration.createWithSuitableUnit(300, TimeUnit.HOURS),
            12, TimeUnit.DAYS);

        check(Duration.createWithSuitableUnit(60 * 24 * 50 + 8, TimeUnit.MINUTES),
            50, TimeUnit.DAYS);

        check(Duration.createWithSuitableUnit(1000L * 60 * 60 * 24 * 7 + 3000, TimeUnit.MILLISECONDS),
            7, TimeUnit.DAYS);

        check(Duration.createWithSuitableUnit(1000L * 60 * 60 * 3 + 1400, TimeUnit.MILLISECONDS),
            3, TimeUnit.HOURS);

        check(Duration.createWithSuitableUnit(248, TimeUnit.SECONDS),
            4, TimeUnit.MINUTES);
    }

    private static void check(Duration duration, long expectedDuration, TimeUnit expectedUnit) {
        assertThat(duration.getTimeUnit(), equalTo(expectedUnit));
        assertThat(duration.getDuration(), equalTo(expectedDuration));
    }
}
