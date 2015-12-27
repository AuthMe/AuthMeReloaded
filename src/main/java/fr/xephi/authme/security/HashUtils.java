package fr.xephi.authme.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public final class HashUtils {

    private HashUtils() {
    }

    public static String sha1(String message) {
        return hash(message, MessageDigestAlgorithm.SHA1);
    }

    public static String sha256(String message) {
        return hash(message, MessageDigestAlgorithm.SHA256);
    }

    public static String sha512(String message) {
        return hash(message, MessageDigestAlgorithm.SHA512);
    }

    public static String md5(String message) {
        return hash(message, MessageDigestAlgorithm.MD5);
    }

    public static MessageDigest getDigest(MessageDigestAlgorithm algorithm) {
        try {
            return MessageDigest.getInstance(algorithm.getKey());
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("Your system seems not to support the hash algorithm '"
                + algorithm.getKey() + "'");
        }
    }

    private static String hash(String message, MessageDigestAlgorithm algorithm) {
        MessageDigest md = getDigest(algorithm);
        md.reset();
        md.update(message.getBytes());
        byte[] digest = md.digest();
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
    }

}
