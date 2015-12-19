package fr.xephi.authme.security;

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Random;

public class RandomString {

    private static final char[] chars = new char[36];
    private static final Random RANDOM = new SecureRandom();

    static {
        for (int idx = 0; idx < 10; ++idx) {
            chars[idx] = (char) ('0' + idx);
        }
        for (int idx = 10; idx < 36; ++idx) {
            chars[idx] = (char) ('a' + idx - 10);
        }
    }

    private final Random random = new Random();

    private final char[] buf;

    public RandomString(int length) {
        if (length < 1)
            throw new IllegalArgumentException("length < 1: " + length);
        buf = new char[length];
        random.setSeed(Calendar.getInstance().getTimeInMillis());
    }

    public String nextString() {
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = chars[random.nextInt(chars.length)];
        return new String(buf);
    }

    public static String generate(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length must be positive but was " + length);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            sb.append(chars[RANDOM.nextInt(chars.length)]);
        }
        return sb.toString();
    }

}
