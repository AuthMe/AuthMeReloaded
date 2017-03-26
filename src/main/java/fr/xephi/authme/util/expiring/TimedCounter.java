package fr.xephi.authme.util.expiring;

import java.util.concurrent.TimeUnit;

/**
 * Keeps a count per key which expires after a configurable amount of time.
 * <p>
 * Once the expiration of an entry has been reached, the counter resets
 * to 0. The counter returns 0 rather than {@code null} for any given key.
 *
 * @param <K> the type of the key
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
     * Decrements the value stored for the provided key.
     * This method will NOT update the expiration.
     *
     * @param key the key to increment the counter for
     */
    public void decrement(K key) {
        ExpiringEntry<Integer> e = getEntries().get(key);

        if (e != null) {
            if (e.getValue() <= 0) {
                remove(key);
            } else {
                getEntries().put(key, new ExpiringEntry<>(e.getValue() - 1, e.getExpiration()));
            }
        }
    }

    /**
     * Calculates the total of all non-expired entries in this counter.
     *
     * @return the total of all valid entries
     */
    public int total() {
        long currentTime = System.currentTimeMillis();
        return getEntries().values().stream()
            .filter(entry -> currentTime <= entry.getExpiration())
            .map(ExpiringEntry::getValue)
            .reduce(0, Integer::sum);
    }
}
