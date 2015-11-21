package fr.xephi.authme.util;

import net.ricecode.similarity.LevenshteinDistanceStrategy;
import net.ricecode.similarity.StringSimilarityService;
import net.ricecode.similarity.StringSimilarityServiceImpl;

/**
 * Utility class for String operations.
 */
public class StringUtils {

    /**
     * Get the difference of two strings.
     *
     * @param first First string
     * @param second Second string
     *
     * @return The difference value
     */
    public static double getDifference(String first, String second) {
        // Make sure the strings are valid.
        if(first == null || second == null)
            return 1.0;

        // Create a string similarity service instance, to allow comparison
        StringSimilarityService service = new StringSimilarityServiceImpl(new LevenshteinDistanceStrategy());

        // Determine the difference value, return the result
        return Math.abs(service.score(first, second) - 1.0);
    }

    /**
     * Returns whether the given string contains any of the provided elements.
     *
     * @param str the string to analyze
     * @param pieces the items to check the string for
     *
     * @return true if the string contains at least one of the items
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

}
