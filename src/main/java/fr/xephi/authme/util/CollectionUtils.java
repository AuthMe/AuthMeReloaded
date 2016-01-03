package fr.xephi.authme.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Utils class for collections.
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    /**
     * Get a range from a list based on start and count parameters in a safe way.
     *
     * @param <T> element
     * @param list The List
     * @param start The start index
     * @param count The number of elements to add
     *
     * @return The sublist consisting at most of {@code count} elements (less if the parameters
     * exceed the size of the list)
     */
    public static <T> List<T> getRange(List<T> list, int start, int count) {
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
     * @param <T> element
     * @param list The List
     * @param start The start index
     *
     * @return The sublist of all elements from index {@code start} and on; empty list
     * if the start index exceeds the list's size
     */
    public static <T> List<T> getRange(List<T> list, int start) {
        if (start >= list.size()) {
            return new ArrayList<>();
        }
        return getRange(list, start, list.size() - start);
    }

    /**
     * @param <T> element
     * @param coll Collection
     * @return boolean Boolean
     */
    public static <T> boolean isEmpty(Collection<T> coll) {
        return coll == null || coll.isEmpty();
    }

    public static <T> List<T> filterCommonStart(List<T> coll1, List<T> coll2) {
        List<T> commonStart = new ArrayList<>();
        int minSize = Math.min(coll1.size(), coll2.size());
        for (int i = 0; i < minSize; ++i) {
            if (Objects.equals(coll1.get(i), coll2.get(i))) {
                commonStart.add(coll1.get(i));
            } else {
                break;
            }
        }
        return commonStart;
    }
}
