package fr.xephi.authme.util.expiring;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.either;
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
        long expiresInHours = set.getExpiration("my entry", TimeUnit.HOURS);
        long expiresInMinutes = set.getExpiration("my entry", TimeUnit.MINUTES);
        long unknownExpires = set.getExpiration("bogus", TimeUnit.SECONDS);

        // then
        assertThat(expiresInHours, equalTo(2L));
        assertThat(expiresInMinutes, either(equalTo(122L)).or(equalTo(123L)));
        assertThat(unknownExpires, equalTo(-1L));
    }

    @Test
    public void shouldReturnMinusOneForExpiredEntry() {
        // given
        ExpiringSet<Integer> set = new ExpiringSet<>(-100, TimeUnit.SECONDS);
        set.add(23);

        // when
        long expiresInSeconds = set.getExpiration(23, TimeUnit.SECONDS);

        // then
        assertThat(expiresInSeconds, equalTo(-1L));
    }
}
