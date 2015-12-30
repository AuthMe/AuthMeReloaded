package fr.xephi.authme.security;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Utility for generating random strings.
 */
public final class RandomString {

    private static final char[] chars = new char[36];
    private static final Random RANDOM = new SecureRandom();
    private static final int HEX_MAX_INDEX = 16;

    static {
        for (int idx = 0; idx < 10; ++idx) {
            chars[idx] = (char) ('0' + idx);
        }
        for (int idx = 10; idx < 36; ++idx) {
            chars[idx] = (char) ('a' + idx - 10);
        }
    }

    private RandomString() {
    }

    /**
     * Generate a string of the given length consisting of random characters within the range [0-9a-z].
     *
     * @param length The length of the random string to generate
     * @return The random string
     */
    public static String generate(int length) {
        return generate(length, chars.length);
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

    private static String generate(int length, int maxIndex) {
        if (length < 0) {
            throw new IllegalArgumentException("Length must be positive but was " + length);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            sb.append(chars[RANDOM.nextInt(maxIndex)]);
        }
        return sb.toString();
    }

}
