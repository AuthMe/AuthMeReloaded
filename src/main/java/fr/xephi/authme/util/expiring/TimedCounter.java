package fr.xephi.authme.util.expiring;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Keeps a count per key which expires after a configurable amount of time.
 * <p>
 * Once the expiration of an entry has been reached, the counter resets
 * to 0. The counter returns 0 rather than {@code null} for any given key.
 */
public class TimedCounter<K> extends ExpiringMap<K, Integer> {

    /**
     * Constructor.
     *
     * @param duration the duration of time after which entries expire
     * @param unit the time unit in which {@code duration} is expressed
     */
    public TimedCounter(long duration, TimeUnit unit) {
        super(duration, unit);
    }

    @Override
    public Integer get(K key) {
        Integer value = super.get(key);
        return value == null ? 0 : value;
    }

    /**
     * Increments the value stored for the provided key.
     *
     * @param key the key to increment the counter for
     */
    public void increment(K key) {
        put(key, get(key) + 1);
    }

    /**
     * Calculates the total of all non-expired entries in this counter.
     *
     * @return the total of all valid entries
     */
    public int total() {
        return entries.values().stream()
            .map(ExpiringEntry::getValue)
            .filter(Objects::nonNull)
            .reduce(0, Integer::sum);
    }
}
