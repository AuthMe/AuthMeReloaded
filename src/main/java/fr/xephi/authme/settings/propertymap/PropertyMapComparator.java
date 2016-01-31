package fr.xephi.authme.settings.propertymap;

import fr.xephi.authme.settings.domain.Property;

import java.util.Comparator;

/**
 * Custom comparator for {@link PropertyMap}. It guarantees that the map's entries:
 * <ul>
 *   <li>are grouped by path, e.g. all "DataSource.mysql" properties are together, and "DataSource.mysql" properties
 *   are within the broader "DataSource" group.</li>
 *   <li>are ordered by insertion, e.g. if the first "DataSource" property is inserted before the first "security"
 *   property, then "DataSource" properties will come before the "security" ones.</li>
 * </ul>
 */
final class PropertyMapComparator implements Comparator<Property> {

    private Node parent = Node.createRoot();

    /**
     * Method to call when adding a new property to the map (as to retain its insertion time).
     *
     * @param property The property that is being added
     */
    public void add(Property property) {
        Node.addNode(parent, property.getPath());
    }

    @Override
    public int compare(Property p1, Property p2) {
        return Node.compare(parent, p1.getPath(), p2.getPath());
    }

}
