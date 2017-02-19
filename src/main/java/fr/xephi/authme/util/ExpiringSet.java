package fr.xephi.authme.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Set whose entries expire after a configurable amount of time. Once an entry
 * has expired, the set will act as if the entry no longer exists. Time starts
 * counting after the entry has been inserted.
 * <p>
 * Internally, expired entries are not cleared automatically. A cleanup can be
 * triggered with {@link #removeExpiredEntries()}. Adding an entry that is
 * already present effectively resets its expiration.
 *
 * @param <E> the type of the entries
 */
public class ExpiringSet<E> {

    private Map<E, Long> entries = new ConcurrentHashMap<>();
    private long expirationMillis;

    /**
     * Constructor.
     *
     * @param duration the duration of time after which entries expire
     * @param unit the time unit in which {@code duration} is expressed
     */
    public ExpiringSet(long duration, TimeUnit unit) {
        setExpiration(duration, unit);
    }

    /**
     * Adds an entry to the set.
     *
     * @param entry the entry to add
     */
    public void add(E entry) {
        entries.put(entry, System.currentTimeMillis() + expirationMillis);
    }

    /**
     * Returns whether this set contains the given entry, if it hasn't expired.
     *
     * @param entry the entry to check
     * @return true if the entry is present and not expired, false otherwise
     */
    public boolean contains(E entry) {
        Long expiration = entries.get(entry);
        return expiration != null && expiration > System.currentTimeMillis();
    }

    /**
     * Removes the given entry from the set (if present).
     *
     * @param entry the entry to remove
     */
    public void remove(E entry) {
        entries.remove(entry);
    }

    /**
     * Removes all entries from the set.
     */
    public void clear() {
        entries.clear();
    }

    /**
     * Removes all entries which have expired from the internal structure.
     */
    public void removeExpiredEntries() {
        entries.entrySet().removeIf(entry -> System.currentTimeMillis() > entry.getValue());
    }

    /**
     * Sets a new expiration duration. Note that already present entries
     * will still make use of the old expiration.
     *
     * @param duration the duration of time after which entries expire
     * @param unit the time unit in which {@code duration} is expressed
     */
    public void setExpiration(long duration, TimeUnit unit) {
        this.expirationMillis = unit.toMillis(duration);
    }

    /**
     * Returns whether this map is empty. This reflects the state of the
     * internal map, which may contain expired entries only. The result
     * may change after running {@link #removeExpiredEntries()}.
     *
     * @return true if map is really empty, false otherwise
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
