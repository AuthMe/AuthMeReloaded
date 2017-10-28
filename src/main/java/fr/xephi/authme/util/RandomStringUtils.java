package fr.xephi.authme.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Utility for generating random strings.
 */
public final class RandomStringUtils {

    private static final char[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final Random RANDOM = new SecureRandom();
    private static final int NUM_INDEX = 10;
    private static final int LOWER_ALPHANUMERIC_INDEX = 36;
    private static final int HEX_MAX_INDEX = 16;

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
        return generateString(length, LOWER_ALPHANUMERIC_INDEX);
    }

    /**
     * Generate a random hexadecimal string of the given length. In other words, the generated string
     * contains characters only within the range [0-9a-f].
     *
     * @param length The length of the random string to generate
     * @return The random hexadecimal string
     */
    public static String generateHex(int length) {
        return generateString(length, HEX_MAX_INDEX);
    }

    /**
     * Generate a random numbers string of the given length. In other words, the generated string
     * contains characters only within the range [0-9].
     *
     * @param length The length of the random string to generate
     * @return The random numbers string
     */
    public static String generateNum(int length) {
        return generateString(length, NUM_INDEX);
    }

    /**
     * Generate a random string with digits and lowercase and uppercase letters. The result of this
     * method matches the pattern [0-9a-zA-Z].
     *
     * @param length The length of the random string to generate
     * @return The random string
     */
    public static String generateLowerUpper(int length) {
        return generateString(length, CHARS.length);
    }

    private static String generateString(int length, int maxIndex) {
        if (length < 0) {
            throw new IllegalArgumentException("Length must be positive but was " + length);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            sb.append(CHARS[RANDOM.nextInt(maxIndex)]);
        }
        return sb.toString();
    }

}
