package fr.xephi.authme.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utils class for collections.
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    /**
     * Get a range from a list based on start and count parameters in a safe way.
     *
     * @param start The start index
     * @param count The number of elements to add
     *
     * @return The sublist consisting at most of {@code count} elements (less if the parameters
     * exceed the size of the list)
     */
    public static List<String> getRange(List<String> list, int start, int count) {
        if (start >= list.size() || count <= 0) {
            return new ArrayList<>();
        } else if (start < 0) {
            start = 0;
        }
        int end = Math.min(list.size(), start + count);
        return list.subList(start, end);
    }

    /**
     * Get all elements from a list starting from the given index.
     *
     * @param start The start index
     *
     * @return The sublist of all elements from index {@code start} and on; empty list
     * if the start index exceeds the list's size
     */
    public static List<String> getRange(List<String> list, int start) {
        if (start >= list.size()) {
            return new ArrayList<>();
        }
        return getRange(list, start, list.size() - start);
    }
}
