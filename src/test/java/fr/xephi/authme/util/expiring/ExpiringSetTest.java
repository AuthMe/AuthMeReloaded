package fr.xephi.authme.util.expiring;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link ExpiringSet}.
 */
public class ExpiringSetTest {

    @Test
    public void shouldAddEntry() {
        // given
        ExpiringSet<String> set = new ExpiringSet<>(10, TimeUnit.MINUTES);

        // when
        set.add("authme");

        // then
        assertThat(set.contains("authme"), equalTo(true));
        assertThat(set.contains("other"), equalTo(false));
    }

    @Test
    public void shouldRemoveEntries() {
        // given
        ExpiringSet<Integer> set = new ExpiringSet<>(20, TimeUnit.SECONDS);
        set.add(20);
        set.add(40);

        // when
        set.remove(40);
        set.remove(60);

        // then
        assertThat(set.contains(20), equalTo(true));
        assertThat(set.contains(40), equalTo(false));
        assertThat(set.contains(60), equalTo(false));
    }

    @Test
    public void shouldHandleNewExpirationAndSupportNegativeValues() {
        // given
        ExpiringSet<Character> set = new ExpiringSet<>(800, TimeUnit.MILLISECONDS);
        set.add('A');

        // when
        set.setExpiration(-10, TimeUnit.SECONDS);
        set.add('Y');

        // then
        assertThat(set.contains('A'), equalTo(true));
        assertThat(set.contains('Y'), equalTo(false));
    }

    @Test
    public void shouldClearAllValues() {
        // given
        ExpiringSet<String> set = new ExpiringSet<>(1, TimeUnit.MINUTES);
        set.add("test");

        // when / then
        assertThat(set.isEmpty(), equalTo(false));
        set.clear();
        assertThat(set.isEmpty(), equalTo(true));
        assertThat(set.contains("test"), equalTo(false));
    }

    @Test
    public void shouldClearExpiredValues() {
        // given
        ExpiringSet<Integer> set = new ExpiringSet<>(2, TimeUnit.HOURS);
        set.add(2);
        set.setExpiration(-100, TimeUnit.SECONDS);
        set.add(3);
        set.setExpiration(20, TimeUnit.MINUTES);
        set.add(6);

        // when
        set.removeExpiredEntries();

        // then
        assertThat(set.contains(2), equalTo(true));
        assertThat(set.contains(3), equalTo(false));
        assertThat(set.contains(6), equalTo(true));
    }

    @Test
    public void shouldReturnExpiration() {
        // given
        ExpiringSet<String> set = new ExpiringSet<>(123, TimeUnit.MINUTES);
        set.add("my entry");

        // when
        Duration expiration = set.getExpiration("my entry");
        Duration unknownExpiration = set.getExpiration("bogus");

        // then
        assertIsDuration(expiration, 2, TimeUnit.HOURS);
        assertIsDuration(unknownExpiration, -1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldReturnExpirationInSuitableUnits() {
        // given
        ExpiringSet<Integer> set = new ExpiringSet<>(601, TimeUnit.SECONDS);
        set.add(12);
        set.setExpiration(49, TimeUnit.HOURS);
        set.add(23);

        // when
        Duration expiration12 = set.getExpiration(12);
        Duration expiration23 = set.getExpiration(23);
        Duration expectedUnknown = set.getExpiration(-100);

        // then
        assertIsDuration(expiration12, 10, TimeUnit.MINUTES);
        assertIsDuration(expiration23, 2, TimeUnit.DAYS);
        assertIsDuration(expectedUnknown, -1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldReturnMinusOneForExpiredEntry() {
        // given
        ExpiringSet<Integer> set = new ExpiringSet<>(-100, TimeUnit.SECONDS);
        set.add(23);

        // when
        Duration expiration = set.getExpiration(23);

        // then
        assertIsDuration(expiration, -1, TimeUnit.SECONDS);
    }

    private static void assertIsDuration(Duration duration, long expectedDuration, TimeUnit expectedUnit) {
        assertThat(duration.getTimeUnit(), equalTo(expectedUnit));
        assertThat(duration.getDuration(), equalTo(expectedDuration));
    }
}
