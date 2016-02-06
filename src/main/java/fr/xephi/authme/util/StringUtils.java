package fr.xephi.authme.util;

import net.ricecode.similarity.LevenshteinDistanceStrategy;
import net.ricecode.similarity.StringSimilarityService;
import net.ricecode.similarity.StringSimilarityServiceImpl;

import java.io.File;
import java.util.Arrays;

/**
 * Utility class for String operations.
 */
public final class StringUtils {

    private StringUtils() {
        // Utility class
    }

    /**
     * Get the difference of two strings.
     *
     * @param first  First string
     * @param second Second string
     *
     * @return The difference value
     */
    public static double getDifference(String first, String second) {
        // Make sure the strings are valid.
        if (first == null || second == null) {
            return 1.0;
        }

        // Create a string similarity service instance, to allow comparison
        StringSimilarityService service = new StringSimilarityServiceImpl(new LevenshteinDistanceStrategy());

        // Determine the difference value, return the result
        return Math.abs(service.score(first, second) - 1.0);
    }

    /**
     * Return whether the given string contains any of the provided elements.
     *
     * @param str    The string to analyze
     * @param pieces The items to check the string for
     *
     * @return True if the string contains at least one of the items
     */
    public static boolean containsAny(String str, String... pieces) {
        if (str == null) {
            return false;
        }
        for (String piece : pieces) {
            if (piece != null && str.contains(piece)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Null-safe method for checking whether a string is empty. Note that the string
     * is trimmed, so this method also considers a string with whitespace as empty.
     *
     * @param str The string to verify
     *
     * @return True if the string is empty, false otherwise
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Join a list of elements into a single string with the specified delimiter.
     *
     * @param delimiter The delimiter to use
     * @param elements  The elements to join
     *
     * @return A new String that is composed of the elements separated by the delimiter
     */
    public static String join(String delimiter, Iterable<String> elements) {
        if (delimiter == null) {
            delimiter = "";
        }
        StringBuilder sb = new StringBuilder();
        for (String element : elements) {
            if (!isEmpty(element)) {
                // Add the separator if it isn't the first element
                if (sb.length() > 0) {
                    sb.append(delimiter);
                }
                sb.append(element);
            }
        }

        return sb.toString();
    }

    /**
     * Join a list of elements into a single string with the specified delimiter.
     *
     * @param delimiter The delimiter to use
     * @param elements  The elements to join
     *
     * @return A new String that is composed of the elements separated by the delimiter
     */
    public static String join(String delimiter, String... elements) {
        return join(delimiter, Arrays.asList(elements));
    }

    /**
     * Format the information from a Throwable as string, retaining the type and its message.
     *
     * @param th The throwable to process
     *
     * @return String with the type of the Throwable and its message, e.g. "[IOException]: Could not open stream"
     */
    public static String formatException(Throwable th) {
        return "[" + th.getClass().getSimpleName() + "]: " + th.getMessage();
    }

    /**
     * Construct a file path from the given elements, i.e. separate the given elements by the file separator.
     *
     * @param elements The elements to create a path with
     *
     * @return The created path
     */
    public static String makePath(String... elements) {
        return join(File.separator, elements);
    }

}
