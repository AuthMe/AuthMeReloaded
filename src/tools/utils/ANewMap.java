package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * A map builder for the lazy.
 * <p />
 * Sample usage:
 * <code>
 *   Map&lt;String, Integer> map = ANewMap
 *     .with("test", 123)
 *     .and("text", 938)
 *     .and("abc", 456)
 *     .build();
 * </code>
 */
public class ANewMap<K, V> {

    private Map<K, V> map = new HashMap<>();

    public static <K, V> ANewMap<K, V> with(K key, V value) {
        ANewMap<K, V> instance = new ANewMap<>();
        return instance.and(key, value);
    }

    public ANewMap<K, V> and(K key, V value) {
        map.put(key, value);
        return this;
    }

    public Map<K, V> build() {
        return map;
    }

}
