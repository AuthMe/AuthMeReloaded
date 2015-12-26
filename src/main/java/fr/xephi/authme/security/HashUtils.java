package fr.xephi.authme.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;


public final class HashUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private HashUtils() {
    }

    public static String hash(String message, MessageDigestAlgorithm algorithm) {
        MessageDigest md = getDigest(algorithm);
        md.reset();
        md.update(message.getBytes());
        byte[] digest = md.digest();
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
    }

    public static String sha1(String message) {
        return hash(message, MessageDigestAlgorithm.SHA1);
    }

    public static String md5(String message) {
        return hash(message, MessageDigestAlgorithm.MD5);
    }

    // Only works for length up to 40!
    public static String generateSalt(int length) {
        byte[] msg = new byte[40];
        RANDOM.nextBytes(msg);
        MessageDigest sha1 = getDigest(MessageDigestAlgorithm.SHA1);
        sha1.reset();
        byte[] digest = sha1.digest(msg);
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest)).substring(0, length);
    }

    public static MessageDigest getDigest(MessageDigestAlgorithm algorithm) {
        try {
            return MessageDigest.getInstance(algorithm.getKey());
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("Your system seems not to support the hash algorithm '"
                + algorithm.getKey() + "'");
        }
    }



}
