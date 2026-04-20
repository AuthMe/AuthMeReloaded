package fr.xephi.authme.util;

import net.ricecode.similarity.LevenshteinDistanceStrategy;
import net.ricecode.similarity.StringSimilarityService;
import net.ricecode.similarity.StringSimilarityServiceImpl;

/**
 * Utility class for String operations.
 */
public final class StringUtils {

    // Utility class
    private StringUtils() {
    }

    /**
     * Calculates the difference of two strings.
     *
     * @param first  first string
     * @param second second string
     * @return the difference value
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
     * Returns whether the given string contains any of the provided elements.
     *
     * @param str    the string to analyze
     * @param pieces the items to check the string for
     * @return true if the string contains at least one of the items
     */
    public static boolean containsAny(String str, Iterable<String> pieces) {
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
     * @param str the string to verify
     * @return true if the string is empty, false otherwise
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Checks that the given needle is in the middle of the haystack, i.e. that the haystack
     * contains the needle and that it is not at the very start or end.
     *
     * @param needle the needle to search for
     * @param haystack the haystack to search in
     * @return true if the needle is in the middle of the word, false otherwise
     */
    // Note ljacqu 20170314: `needle` is restricted to char type intentionally because something like
    // isInsideString("11", "2211") would unexpectedly return true...
    public static boolean isInsideString(char needle, String haystack) {
        int index = haystack.indexOf(needle);
        return index > 0 && index < haystack.length() - 1;
    }
}
