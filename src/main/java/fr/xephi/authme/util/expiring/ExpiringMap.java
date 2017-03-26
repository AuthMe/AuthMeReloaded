package fr.xephi.authme.util.expiring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Map with expiring entries. Following a configured amount of time after
 * an entry has been inserted, the map will act as if the entry does not
 * exist.
 * <p>
 * Time starts counting directly after insertion. Inserting a new entry with
 * a key that already has a value will "reset" the expiration. Although the
 * expiration can be redefined later on, only entries which are inserted
 * afterwards will use the new expiration.
 * <p>
 * An expiration of {@code <= 0} will make the map expire all entries
 * immediately after insertion. Note that the map does not remove expired
 * entries automatically; this is only done when calling
 * {@link #removeExpiredEntries()}.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class ExpiringMap<K, V> {

    private final Map<K, ExpiringEntry<V>> entries = new ConcurrentHashMap<>();
    private long expirationMillis;

    /**
     * Constructor.
     *
     * @param duration the duration of time after which entries expire
     * @param unit the time unit in which {@code duration} is expressed
     */
    public ExpiringMap(long duration, TimeUnit unit) {
        setExpiration(duration, unit);
    }

    /**
     * Returns the value associated with the given key,
     * if available and not expired.
     *
     * @param key the key to look up
     * @return the associated value, or {@code null} if not available
     */
    public V get(K key) {
        ExpiringEntry<V> value = entries.get(key);
        if (value == null) {
            return null;
        } else if (System.currentTimeMillis() > value.getExpiration()) {
            entries.remove(key);
            return null;
        }
        return value.getValue();
    }

    /**
     * Inserts a value for the given key. Overwrites a previous value
     * for the key if it exists.
     *
     * @param key the key to insert a value for
     * @param value the value to insert
     */
    public void put(K key, V value) {
        long expiration = System.currentTimeMillis() + expirationMillis;
        entries.put(key, new ExpiringEntry<>(value, expiration));
    }

    /**
     * Removes the value for the given key, if available.
     *
     * @param key the key to remove the value for
     */
    public void remove(K key) {
        entries.remove(key);
    }

    /**
     * Removes all entries which have expired from the internal structure.
     */
    public void removeExpiredEntries() {
        entries.entrySet().removeIf(entry -> System.currentTimeMillis() > entry.getValue().getExpiration());
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

    /**
     * @return the internal map
     */
    protected Map<K, ExpiringEntry<V>> getEntries() {
        return entries;
    }

    /**
     * Class holding a value paired with an expiration timestamp.
     *
     * @param <V> the value type
     */
    protected static final class ExpiringEntry<V> {

        private final V value;
        private final long expiration;

        ExpiringEntry(V value, long expiration) {
            this.value = value;
            this.expiration = expiration;
        }

        V getValue() {
            return value;
        }

        long getExpiration() {
            return expiration;
        }
    }
}
