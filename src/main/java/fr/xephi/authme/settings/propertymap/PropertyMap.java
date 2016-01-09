package fr.xephi.authme.settings.propertymap;

import fr.xephi.authme.settings.domain.Property;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class wrapping a {@code Map<Property, String[]>} for storing properties and their associated
 * comments with custom ordering.
 *
 * @see PropertyMapComparator for details about the map's order
 */
public class PropertyMap {

    private Map<Property<?>, String[]> map;
    private PropertyMapComparator comparator;

    /**
     * Create a new property map.
     */
    public PropertyMap() {
        comparator = new PropertyMapComparator();
        map = new TreeMap<>(comparator);
    }

    /**
     * Add a new property to the map.
     *
     * @param property The property to add
     * @param comments The comments associated to the property
     */
    public void put(Property property, String[] comments) {
        comparator.add(property);
        map.put(property, comments);
    }

    /**
     * Return the entry set of the map.
     *
     * @return The entry set
     */
    public Set<Map.Entry<Property<?>, String[]>> entrySet() {
        return map.entrySet();
    }

    /**
     * Return the key set of the map, i.e. all property objects it holds.
     *
     * @return The key set
     */
    public Set<Property<?>> keySet() {
        return map.keySet();
    }

    /**
     * Return the size of the map.
     *
     * @return The size
     */
    public int size() {
        return map.size();
    }

}
