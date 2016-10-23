package fr.xephi.authme.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Utility for generating random strings.
 */
public final class RandomStringUtils {

    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Random RANDOM = new SecureRandom();
    private static final int HEX_MAX_INDEX = 16;
    private static final int LOWER_ALPHANUMERIC_INDEX = 36;

    // Utility class
    private RandomStringUtils() {
    }

    /**
     * Generate a string of the given length consisting of random characters within the range [0-9a-z].
     *
     * @param length The length of the random string to generate
     * @return The random string
     */
    public static String generate(int length) {
        return generate(length, LOWER_ALPHANUMERIC_INDEX);
    }

    /**
     * Generate a random hexadecimal string of the given length. In other words, the generated string
     * contains characters only within the range [0-9a-f].
     *
     * @param length The length of the random string to generate
     * @return The random hexadecimal string
     */
    public static String generateHex(int length) {
        return generate(length, HEX_MAX_INDEX);
    }

    /**
     * Generate a random string with digits and lowercase and uppercase letters. The result of this
     * method matches the pattern [0-9a-zA-Z].
     *
     * @param length The length of the random string to generate
     * @return The random string
     */
    public static String generateLowerUpper(int length) {
        return generate(length, CHARS.length());
    }

    private static String generate(int length, int maxIndex) {
        if (length < 0) {
            throw new IllegalArgumentException("Length must be positive but was " + length);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            sb.append(CHARS.charAt(RANDOM.nextInt(maxIndex)));
        }
        return sb.toString();
    }

}
