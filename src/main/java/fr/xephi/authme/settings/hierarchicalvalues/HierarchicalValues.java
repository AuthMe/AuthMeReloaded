package fr.xephi.authme.settings.hierarchicalvalues;

import fr.xephi.authme.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Container for values which are inherited from parents but can be overridden at arbitrary levels.
 * Never returns null for any {@link #getValue value lookup} if the fallback provided on initialization
 * is not null.
 *
 * @param <T> the value type
 */
public class HierarchicalValues<T> {

    private final Map<String, T> values;
    private boolean isRootAbsent;

    private HierarchicalValues(Map<String, T> values, boolean isRootAbsent) {
        this.values = values;
        this.isRootAbsent = isRootAbsent;
    }

    public static <T> HierarchicalValues<T> createContainerWithRoot(T value) {
        Map<String, T> values = new HashMap<>();
        values.put("", value);
        return new HierarchicalValues<>(values, false);
    }

    public static <T> HierarchicalValues<T> createContainer(T rootValueFallback, Map<String, T> values) {
        Map<String, T> valuesInternal = new HashMap<>(values);

        boolean isRootAbsent = false;
        if (valuesInternal.get("") == null) {
            valuesInternal.put("", rootValueFallback);
            isRootAbsent = true;
        }
        return new HierarchicalValues<>(valuesInternal, isRootAbsent);
    }

    /**
     * Returns the most specific value for the given key.
     *
     * @param key the key whose value should be fetched
     * @return value applicable to the key
     */
    public T getValue(String key) {
        if (StringUtils.isEmpty(key)) {
            return values.get("");
        }

        T value = values.get(key);
        if (value == null) {
            return getValue(createParentKey(key));
        } else {
            return value;
        }
    }

    /**
     * Adds the given value for the provided key.
     *
     * @param key the key to add (or replace) the value for
     * @param value the value to set
     */
    public void addValue(String key, T value) {
        values.put(key, value);

        if (isRootAbsent && "".equals(key)) {
            isRootAbsent = false;
        }
    }

    public boolean hasSpecificValueForKey(String key) {
        return values.containsKey(key);
    }

    /**
     * Returns a stream of all the values for which a value was specified.
     *
     * @return stream of the specific entries
     */
    public Stream<Map.Entry<String, T>> createValuesStream() {
        if (isRootAbsent) {
            return values.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(""));
        }
        return values.entrySet().stream();
    }

    /**
     * Returns the direct parent of the given key, e.g. "authme.settings" for "authme.settings.properties".
     *
     * @param key the key to create the parent of
     * @return the parent key
     */
    private static String createParentKey(String key) {
        int lastDotIndex = Math.max(key.lastIndexOf('.'), 0);
        return key.substring(0, lastDotIndex);
    }
}
