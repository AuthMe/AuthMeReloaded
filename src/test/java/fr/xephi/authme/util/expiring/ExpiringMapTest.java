package fr.xephi.authme.util.expiring;

import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link ExpiringMap}.
 */
public class ExpiringMapTest {

    @Test
    public void shouldAddAndRetrieveEntries() {
        // given
        ExpiringMap<String, Double> map = new ExpiringMap<>(3, TimeUnit.MINUTES);

        // when / then
        map.put("three", 3.0);
        map.put("treefiddy", 3.50);

        assertThat(map.get("three"), equalTo(3.0));
        assertThat(map.get("treefiddy"), equalTo(3.50));
    }

    @Test
    public void shouldRemoveEntry() {
        // given
        ExpiringMap<String, Boolean> map = new ExpiringMap<>(1, TimeUnit.HOURS);
        map.put("hi", true);
        map.put("ha", false);

        // when
        map.remove("ha");

        // then
        assertThat(map.get("ha"), nullValue());
        assertThat(map.get("hi"), equalTo(true));
    }

    @Test
    public void shouldUpdateExpirationAndSupportNegativeValues() {
        // given
        ExpiringMap<Integer, Integer> map = new ExpiringMap<>(2, TimeUnit.DAYS);
        map.put(2, 4);
        map.put(3, 9);

        // when
        map.setExpiration(-100, TimeUnit.MILLISECONDS);

        // then
        map.put(5, 25);
        assertThat(map.get(2), equalTo(4));
        assertThat(map.get(3), equalTo(9));
        assertThat(map.get(5), nullValue());
    }

    @Test
    public void shouldCleanUpExpiredEntries() throws InterruptedException {
        // given
        ExpiringMap<Integer, Integer> map = new ExpiringMap<>(200, TimeUnit.MILLISECONDS);
        map.put(144, 12);
        map.put(121, 11);
        map.put(81, 9);
        map.setExpiration(900, TimeUnit.MILLISECONDS);
        map.put(64, 8);
        map.put(25, 5);

        // when
        Thread.sleep(300);
        map.removeExpiredEntries();

        // then
        Map<Integer, ?> internalMap = map.getEntries();
        assertThat(internalMap.keySet(), containsInAnyOrder(64, 25));
    }

    @Test
    public void shouldReturnIfIsEmpty() {
        // given
        ExpiringMap<String, String> map = new ExpiringMap<>(-8, TimeUnit.SECONDS);

        // when / then
        assertThat(map.isEmpty(), equalTo(true));
        map.put("hoi", "Welt");
        assertThat(map.isEmpty(), equalTo(false));
        map.removeExpiredEntries();
        assertThat(map.isEmpty(), equalTo(true));
    }
}
