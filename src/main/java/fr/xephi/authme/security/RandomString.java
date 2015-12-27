package fr.xephi.authme.security;

import java.security.SecureRandom;
import java.util.Random;

public final class RandomString {

    private static final char[] chars = new char[36];
    private static final Random RANDOM = new SecureRandom();
    private static final int HEX_MAX_INDEX = 15;

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

    public static String generate(int length) {
        return generate(length, chars.length);
    }

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
