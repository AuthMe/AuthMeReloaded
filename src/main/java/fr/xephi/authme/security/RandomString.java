package fr.xephi.authme.security;

import java.util.Random;

/**
 *
 * @author Xephi59
 */
public class RandomString {

    private static final char[] chars = new char[36];

    static {
        for (int idx = 0; idx < 10; ++idx)
            chars[idx] = (char) ('0' + idx);
        for (int idx = 10; idx < 36; ++idx)
            chars[idx] = (char) ('a' + idx - 10);
    }

    private final Random random = new Random();

    private final char[] buf;

    public RandomString(int length) {
        if (length < 1)
            throw new IllegalArgumentException("length < 1: " + length);
        buf = new char[length];
    }

    public String nextString() {
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = chars[random.nextInt(chars.length)];
        return new String(buf);
    }

}
