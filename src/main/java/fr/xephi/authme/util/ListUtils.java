package fr.xephi.authme.util;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {

    /**
     * Implode a list of elements into a single string, with a specified separator.
     *
     * @param elements The elements to implode.
     * @param separator The separator to use.
     *
     * @return The result string.
     */
    public static String implode(List<String> elements, String separator) {
        // Create a string builder
        StringBuilder sb = new StringBuilder();

        // Append each element
        for(String element : elements) {
            // Make sure the element isn't empty
            if(element.trim().length() == 0)
                continue;

            // Prefix the separator if it isn't the first element
            if(sb.length() > 0)
                sb.append(separator);

            // Append the element
            sb.append(element);
        }

        // Return the result
        return sb.toString();
    }

    /**
     * Implode two lists of elements into a single string, with a specified separator.
     *
     * @param elements The first list of elements to implode.
     * @param otherElements The second list of elements to implode.
     * @param separator The separator to use.
     *
     * @return The result string.
     */
    public static String implode(List<String> elements, List<String> otherElements, String separator) {
        // Combine the lists
        List<String> combined = new ArrayList<>();
        combined.addAll(elements);
        combined.addAll(otherElements);

        // Implode and return the result
        return implode(combined, separator);
    }

    /**
     * Implode two elements into a single string, with a specified separator.
     *
     * @param element The first element to implode.
     * @param otherElement The second element to implode.
     * @param separator The separator to use.
     *
     * @return The result string.
     */
    public static String implode(String element, String otherElement, String separator) {
        // Combine the lists
        List<String> combined = new ArrayList<>();
        combined.add(element);
        combined.add(otherElement);

        // Implode and return the result
        return implode(combined, separator);
    }
}
